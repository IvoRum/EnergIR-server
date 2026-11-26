# Groups API — nested groups + many-to-many membership

Status: **implemented and deployed** (server `dev`, `93a145d`).
Audience: the agent implementing the UI. Everything below is already live; the UI only needs to consume it.

---

## 1. Concept

A **group** is a node in a per-module tree. A group can contain:

* **members** — the objects of the module (meters, compressors, products…), many-to-many;
* **sub-groups** — other groups of the same module (arbitrary depth).

So one group node holds both kinds of children at once, e.g.:

```
Factory
├── Compressor room          (group)
│   ├── Compressors          (group)
│   │   ├── [compressor 3]
│   │   └── [compressor 7]
│   └── [compressor 9]       (member directly on "Compressor room")
└── [compressor 12]
```

Rules enforced by the server:

* a member may be in **0..N groups** of its module;
* a group may have **0..N sub-groups**;
* a group **cannot** become a child of itself or of its own descendant → `400`;
* deleting a group **re-parents** its sub-groups to its parent and removes its memberships; **members are never deleted**;
* archiving hides a group without touching its data.

One model serves every module. The module is a path parameter, never mixed.

---

## 2. Modules

`module` is one of: `ELECTRIC`, `WATER`, `GAS`, `PRODUCTION`, `COMPRESSOR`.

`memberId` is interpreted per module:

| module | `memberId` refers to | source of ids |
|---|---|---|
| `ELECTRIC` | `electric_meter.id` | `GET /api/v1/meters` |
| `WATER` | `water_meter.id` | `GET /api/v1/water` |
| `GAS` | `gas_meter.id` | `GET /api/v1/gas` |
| `PRODUCTION` | `product.id` | `GET /api/v1/production` |
| `COMPRESSOR` | `compressor.id` | `GET /api/v1/compressors` |

The server does **not** validate that `memberId` exists (it is module-agnostic); the UI should only offer ids it already has.

---

## 3. Endpoints

Base: `/api/v1/groups`. All are company-scoped from the JWT; `module` is always required.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/groups?module=WATER` | flat list of the module's groups |
| `POST` | `/api/v1/groups?module=WATER` | create |
| `PUT` | `/api/v1/groups/{id}?module=WATER` | partial update, incl. move |
| `PUT` | `/api/v1/groups/{id}/archive?module=WATER` | archive |
| `PUT` | `/api/v1/groups/{id}/unarchive?module=WATER` | restore |
| `DELETE` | `/api/v1/groups/{id}?module=WATER` | delete (see rules above) |
| `PUT` | `/api/v1/groups/{id}/members/{memberId}?module=WATER` | attach member (idempotent) |
| `DELETE` | `/api/v1/groups/{id}/members/{memberId}?module=WATER` | detach member |

### Response object (`GroupNodeResponse`)

```json
{
  "id": 12,
  "module": "COMPRESSOR",
  "name": "Compressor room",
  "description": "Hall A",
  "color": "#3b82f6",
  "parentId": 3,
  "archived": false,
  "createdOn": "2026-11-26",
  "memberIds": [3, 7, 9]
}
```

The list endpoint returns **every node flat**; build the tree client-side with `parentId` (`null` = root). Do not rely on ordering.

### Create

`POST /api/v1/groups?module=COMPRESSOR`

```json
{ "name": "Compressor room", "description": "Hall A", "color": "#3b82f6", "parentId": 3 }
```

`parentId` may be omitted or `null` for a root group. Returns `201` + the created object.

### Update / move

`PUT /api/v1/groups/12?module=COMPRESSOR` — send only what changes:

```json
{ "name": "Compressor room A", "color": "#10b981", "parentId": 5 }
```

`parentId` semantics:

| value | effect |
|---|---|
| omitted / `null` | keep current parent |
| `0` | move to root |
| `> 0` | move under that group |

Invalid moves return `400` with a readable message (`A group cannot be its own parent`, `Cannot move a group inside its own sub-group`).

### Attach / detach a member

`PUT /api/v1/groups/12/members/9?module=COMPRESSOR` → `200` + the group (with updated `memberIds`).
Idempotent: attaching twice is a no-op. `DELETE` on the same path detaches.

Both return the updated group, so the UI can re-render from the response without a refetch.

---

## 4. UI requirements

1. **Tree view per module.** Build from the flat list: `Map<parentId, nodes>`. Render sub-groups and members under each node; collapse/expand per node; show a color dot and counts (`n groups · m items`).
2. **Create** — either at root or "New sub-group" on a node (pre-fills `parentId`).
3. **Rename / recolor / describe** — `PUT` with the changed fields only.
4. **Move** (drag & drop or a "Move to…" picker): block dropping a node onto itself or any of its descendants **client-side too** (the server rejects it, but a blocked drop is better UX). After a move, refresh from `GET`.
5. **Membership editor on the item side** — for every meter/compressor/product, a multi-select "Groups" control listing that module's groups (checkboxes). Check → `PUT .../members/{id}`; uncheck → `DELETE ...`. Show "No groups" as an empty state.
6. **Archive** — hidden by default; a "Show archived" toggle reveals them. Archived groups keep their subtree and members.
7. **Delete** — confirm with the group name and explain that sub-groups move up and items are not deleted. Then refetch.
8. **Empty / error states** — `modules` with no groups → "No groups yet"; `400` from a move → toast with the server message; `404` → refetch (stale node).

### Migration notes for the UI

* **Compressors**: groups currently live in `localStorage` (`compressor-groups` + `compressor-group-map`). On first load after this release, offer a one-time "Import existing groups" action: create each local group via `POST /groups?module=COMPRESSOR` and attach its mapped compressors via `PUT .../members/{id}`. After a successful import, stop reading `localStorage`.
* **Water / gas / production**: each item still has a legacy single `groupId`. Show `memberIds` from the new API as the source of truth. When the item has a legacy `groupId` but no memberships, the UI may pre-check that group (so existing data is not hidden), and the next save writes through the new endpoints.
* **Electric**: there were no groups before, so the tree starts empty.

### Server-side migration of legacy groups (run once, before/with the UI release)

`water_group`, `gas_group`, `product_group` rows are **not** yet copied into `group_node`, and their meters' `group_id` are not yet copied into `group_member`. Until that runs, the new API returns an empty tree for those modules. Because legacy ids collide across modules, migrate with a mapping (repeat per module):

```sql
DO $$
DECLARE r record; new_id integer;
BEGIN
  FOR r IN SELECT * FROM energir.water_group LOOP
    new_id := nextval('energir.energir_group_node_seq');
    INSERT INTO energir.group_node (id, company_id, module, name, description, color, parent_id, archived, created_on)
    VALUES (new_id, r.company_id, 'WATER', r.name, r.description, r.color, NULL, false, now());

    INSERT INTO energir.group_member (id, group_id, member_id, created_on)
    SELECT nextval('energir.energir_group_member_seq'), new_id, m.id, now()
    FROM energir.water_meter m
    WHERE m.group_id = r.id;
  END LOOP;
END $$;
```

Same block for `gas_group`/`gas_meter` and `product_group`/`product`. Legacy tables can stay (read-only) or be dropped afterwards; the new tables are the source of truth.

---

## 5. Acceptance checklist

- [ ] Create two root groups, one inside the other, and a member on each level.
- [ ] The same item appears in two groups; removing it from one keeps it in the other.
- [ ] Drag a parent into its own child → blocked (client) and `400` (server).
- [ ] Move a group to root via `parentId: 0`.
- [ ] Delete a group with sub-groups → sub-groups survive at the deleted group's parent; members remain and are not deleted.
- [ ] Archive/unarchive hides and restores the subtree.
- [ ] `GET` after every mutation reflects the change (no stale `memberIds`).
- [ ] A module with no groups shows an empty state, not an error.
- [ ] Legacy `groupId` data is visible (pre-checked) and migrates on first save.
