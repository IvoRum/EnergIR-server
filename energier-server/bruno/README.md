# EnergIR Server — Bruno collection

An [Bruno](https://www.usebruno.com/) API collection covering **every endpoint**
exposed by this service.

## Import

1. Open Bruno → **Collection** → **Open Collection**.
2. Select this `bruno` folder (`energier-server/bruno`).
3. Pick the **Local** environment (top-right) — base URL `http://localhost:8080`.
4. Set the `password` environment variable yourself (Bruno does not commit one):
   environment editor → add `password`, or in the CLI:
   `npx @usebruno/cli run --env Local --env-var password='<your test password>'`.
5. Run **`1. Auth / 01 Register`** then **`02 Authenticate`** first; the token is
   saved to the `token` environment variable automatically.

Or run it headless (the CLI must be run **from the collection root**):

```bash
cd energier-server/bruno
npx @usebruno/cli run --env Local --env-var password='<your test password>'
```

## What's inside

| Folder | Request | Method | Path |
|---|---|---|---|
| 1. Auth | Register | POST | `/api/vi/auth/register` |
| 1. Auth | Authenticate | POST | `/api/vi/auth/authenticate` |
| 2. Electric Meters | Get All Meters | GET | `/api/v1/meters` |
| 2. Electric Meters | Get Meter By Id | GET | `/api/v1/meters/{id}` |
| 2. Electric Meters | Create Meter | POST | `/api/v1/meters` |
| 2. Electric Meters | Update Meter | PUT | `/api/v1/meters/{id}` |
| 3. Electric Meter Readings | Create Reading | POST | `/api/v1/readings` |
| 3. Electric Meter Readings | Get Meter Readings | GET | `/api/v1/meters/{meterId}/readings` |

Folders are numbered so *Run Collection* executes them in dependency order
(auth → meter → reading) on a fresh database.

## Environment variables (`environments/Local.bru`)

| Variable | Default | Notes |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | |
| `email` | `demo@volts.test` | seeded test account |
| `password` | *(not committed)* | set it locally; see below |
| `token` | *(auto-filled)* | set by the auth requests |
| `meterId` | `1` | auto-filled by *Create Meter* |
| `edgeId` / `edgeMeterId` | `edge-test-1` / `42` | meter identity tuple |

## Notes

- **The auth path is `/api/vi/auth`** — a typo for `v1` in
  `AuthenticationController.java`, matched by `SecurityConfig`. Use it as-is.
- `Instant` fields (`createdOn`) come back as **epoch seconds** (e.g.
  `1792126347.05915`), not ISO strings.
- `Register` returns **303** (empty) when the email already exists — the request's
  test accepts either 200 or 303, so re-running is safe.
- `POST /api/v1/meters` is idempotent on `(edgeId, edgeMeterId)` — the
  *Create Meter* test therefore asserts identity, not a fresh row.
- `GET /api/v1/meters/{id}/readings` is **oldest-first**; `since` is included but
  disabled — toggle it on to filter.
- Requests require `Authorization: Bearer {{token}}`; without it the server
  returns **403** (not 401).
- Verified against a running server: **8/8 requests, 16/16 tests pass**, and the
  collection is safe to re-run.
