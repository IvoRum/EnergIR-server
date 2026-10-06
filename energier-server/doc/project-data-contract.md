# Edge Electric Energy — Project & Data Contract Specification

**Status:** descriptive specification of the system **as it exists in the repository today**
(commit `d93f9ae`, 2026-09-29; verified against the running schema and the live
`app/repository/meter.db` on 2026-10-06).

**Purpose:** this document is the single reference for building the **server (central/cloud) side**
of this product. Everything below describes a contract the edge device already emits or consumes.
Where the current implementation is inconsistent, broken, or self-contradicting, that is called out
explicitly rather than silently normalised — a server built against an idealised version of these
contracts will not interoperate with the device that is actually deployed.

**Reading rules for the server implementer:**

- Field names, key casing, types and units in this document are **literal**. `Modbuss_RTU` really
  has two `s`. `GET /electric/{meter_id}}` really has two closing braces. `"Simens"` is really
  spelled that way in the data.
- Anything marked **[AS-IS DEFECT]** is a contract the device currently emits incorrectly. Decide
  per item whether the server tolerates it, rejects it, or whether the edge gets fixed first.
- Anything marked **[PROTECTED]** is Modbus register-parsing code that the project owner has
  forbidden changing (see `app/documentation/modbus-do-not-modify.md`). The server must adapt to it;
  it will not be changed to suit the server.
- The desktop dashboard (`app/ui/`) is **out of scope** for this document by request. Note only that
  it writes directly to the same SQLite file (meters and time sheets), so the server cannot assume
  the HTTP API is the only writer.

---

## Table of contents

1. [System overview and topology](#1-system-overview-and-topology)
2. [Runtime and deployment facts](#2-runtime-and-deployment-facts)
3. [Identifier semantics — read this before anything else](#3-identifier-semantics--read-this-before-anything-else)
4. [Relational data model (SQLite)](#4-relational-data-model-sqlite)
5. [Current data inventory](#5-current-data-inventory)
6. [Modbus acquisition layer](#6-modbus-acquisition-layer)
7. [In-process type definitions (DTOs and entities)](#7-in-process-type-definitions-dtos-and-entities)
8. [HTTP API contract](#8-http-api-contract)
9. [GraphQL contract](#9-graphql-contract)
10. [Configuration contract (`settings.json`)](#10-configuration-contract-settingsjson)
11. [Scheduler / polling contract](#11-scheduler--polling-contract)
12. [Excel export contract](#12-excel-export-contract)
13. [Error and status-code contract](#13-error-and-status-code-contract)
14. [Cross-cutting contract hazards](#14-cross-cutting-contract-hazards)
15. [Requirements this imposes on the server](#15-requirements-this-imposes-on-the-server)
16. [Appendix A — canonical JSON payloads](#appendix-a--canonical-json-payloads)
17. [Appendix B — full DDL](#appendix-b--full-ddl)
18. [Appendix C — file and route inventory](#appendix-c--file-and-route-inventory)

---

## 1. System overview and topology

The product is an **edge data-acquisition node** for industrial electricity metering.

```
  ┌──────────────────────────┐
  │ Siemens Sentron PAC3120  │  × 10 meters, RS-485 multidrop bus
  │ energy meters            │  Modbus RTU slave addresses 1..10
  └─────────────┬────────────┘
                │ RS-485 / Modbus RTU (9600 8N1, 1 s timeout)
                │ USB-to-RS485 adapter: COM2/COM3 (Windows) or /dev/ttyUSB0 (Linux)
  ┌─────────────┴────────────┐
  │ EDGE NODE  (this repo)   │
  │                          │
  │  FastAPI "Modbus API     │   • polls meters on a per-meter schedule
  │  Service" on :8000       │   • persists readings to local SQLite
  │  + schedule-lib thread   │   • exposes live + stored data over HTTP
  │  + SQLite meter.db       │   • appends every reading to an .xlsx file
  │  + openpyxl Excel export │   • no authentication, no TLS, no outbound sync
  └─────────────┬────────────┘
                │
     ┌──────────┼───────────────────────────┬──────────────────────────┐
     │          │                           │                          │
  HTTP/JSON  SQLite file (direct read)   .xlsx file            ** SERVER (to be built) **
  web UI     edge-monitoring Java CLI    OneDrive folder       no contract exists yet
  :3000      (read-only diagnostics)     (manual/BI consumption)
```

### What exists today

| Component | Path | Role |
|---|---|---|
| FastAPI service | `app/main.py` + `app/router/` | The only network interface. Title: `Modbus API Service`. |
| Modbus reader | `app/reading/reading_logic.py` | **[PROTECTED]** register reads + IEEE-754 decoding. |
| Business services | `app/service/` | Aggregation, settings I/O, the polling scheduler. |
| Persistence | `app/repository/` | Raw `sqlite3`, no ORM. |
| Schema + seed SQL | `app/resources/scripts/` | `create.sql`, `insert.sql`, `drop.sql`, `clients/*.sql`. |
| Startup init | `app/init/` | Runs `create.sql`, then the client seed script. |
| API collection | `bruno/` | Bruno requests mirroring every endpoint. **Authoritative client reference.** |
| Monitoring CLI | `edge-monitoring/` | Java/picocli read-only diagnostics over the same SQLite file. |
| Abandoned API collection | `Endpoints/` | **Stale duplicate of `bruno/`. Ignore it.** |

### What does not exist today — and is what the server must supply

- **No outbound communication of any kind.** No HTTP client, no message queue, no replication, no
  file upload. `grep` for `requests`/`httpx`/`urllib` in `app/` returns nothing. All data leaves the
  edge only by someone pulling it over the local HTTP API, by reading the SQLite file, or via the
  Excel file landing in a OneDrive-synced folder.
- **No authentication, authorisation, or transport security.** Every endpoint is anonymous plain
  HTTP. There is no device identity, no tenant/site identifier, and no API key anywhere in the
  codebase or configuration.
- **No site/tenant/device identity in the data model.** Readings are keyed only by a local
  `electric_meter.id`, which is a per-device autoincrement starting at 1. Two edge nodes both have a
  meter `id = 1`. Nothing in any payload says *which* edge node produced it.
- **The configuration hook for a server already exists but is dead.** `settings.json` carries a
  `Remote_Database` section (`url`, `user`, `password`, `db_name`) that is parsed, held in memory,
  and served over `GET /settings/` — but **no code reads it**. Its current value in the repo is
  `url: "Hello!"`. This is the natural insertion point for server connection details.

---

## 2. Runtime and deployment facts

| Fact | Value |
|---|---|
| Language / runtime | Python. README says 3.10+; the checked-in bytecode is `cpython-314` (3.14). |
| Web framework | FastAPI + Uvicorn |
| ASGI app object | `main:app` |
| Default bind | `0.0.0.0:8000` (`ui/run_dashboard.py`, `run_api.sh`); `uvicorn main:app --reload` for dev |
| Required working directory | **`app/`** — mandatory, see below |
| Dependencies | `fastapi`, `uvicorn`, `pymodbus`, `strawberry-graphql`, `pyserial`, `openpyxl`, `schedule`, `customtkinter` (`app/requirements.txt`) |
| Database | SQLite file, `app/repository/meter.db`, created on startup if absent |
| OpenAPI | `GET /openapi.json`, Swagger at `GET /docs`, ReDoc at `GET /redoc` |
| Known LAN address | `192.168.100.12` (Bruno `LAN` environment, CORS origin list) |
| Remote access | WireGuard VPN, subnet `10.0.0.0/24`, Windows `10.0.0.1` ↔ Debian `10.0.0.2` (`docs/vpn-setup.md`) |
| Linux deployment | `docs/debian-setup.md`, `run_api.sh`, venv at `app/.venv-linux` |

### Working-directory dependency

`app/router/constants.py:5` hardcodes `DB_LOCATION = "repository/meter.db"` — a **relative** path.
Several other modules do the same. Other modules (`main.py`, `repository/error_log_repository.py`,
`service/timesheet/time_sheet_service.py:84`) anchor the same file with `Path(__file__).parent…`.
Consequence: starting the process from anywhere other than `app/` makes the HTTP read paths open a
**different, empty** SQLite file than the one the scheduler writes to, with no error. The zero-byte
`app/meter.db` in the repo is evidence of this having happened.

**Server implication:** if the server ever instructs the edge to restart, or ships a service unit,
the working directory must be `app/`.

### CORS

`app/main.py:53-64` — `allow_origins` is an explicit allowlist:

```
http://localhost:3000
http://192.168.100.12:3000
```

with `allow_credentials=True`, `allow_methods=["*"]`, `allow_headers=["*"]`. A browser-based server
console on any other origin will be blocked until that list changes. Server-to-server calls are
unaffected (CORS is browser-enforced).

### Startup sequence (`main.py` lifespan)

1. `init_db(app/repository/meter.db, app/resources/scripts/create.sql)` — `executescript` of the
   full schema (all statements are `CREATE TABLE IF NOT EXISTS`, so it is idempotent).
2. `client_set_up(...)` — `executescript` of `app/resources/scripts/clients/markely_insert.sql`
   (idempotent: every insert is guarded by `WHERE NOT EXISTS`).
3. `activate_time_sheets_service_con(conn)` — registers polling jobs and performs one immediate
   "control read" per meter.
4. `start_scheduler_thread()` — starts the daemon thread running `schedule.run_pending()` every 1 s.

Steps 1 and 2 **swallow all exceptions** and only `print()` them (`init/init_db.py:18`,
`init/markely_init.py:20`). A broken schema or missing seed file leaves the app running against a
partially-initialised database; the first visible symptom is an unrelated `no such table` error from
some endpoint later. Step 3 failure is caught, printed, *and* written to `error_log` with
`status='critical'`.

---

## 3. Identifier semantics — read this before anything else

This is the single largest source of contract errors in the existing code, and the thing a server
is most likely to get wrong. There are **two different numeric identifiers per meter**, they are
both small integers, and in the current deployment they happen to have the same values.

| Identifier | Source | Meaning | Range today |
|---|---|---|---|
| `electric_meter.id` | SQLite `INTEGER PRIMARY KEY AUTOINCREMENT` | Local database primary key. FK target for `electric_meter_energy_readings.meter_id` and `time_sheet.meter_id`. | 1..10 |
| `electric_meter.address` | Set when the meter is registered | **Modbus RTU slave address** — the physical device address on the RS-485 bus. Passed to pymodbus as `device_id`. | 1..10 |

In `app/repository/meter.db` right now, `id == address` for all ten meters. **This is a
coincidence of the seed script**, not a constraint. Nothing in the schema enforces it, and
`POST /meters` lets you create a meter with any `(name, address, meter_config)` combination.

### Which identifier each surface uses

| Surface | Field | Which identifier | Notes |
|---|---|---|---|
| `electric_meter_energy_readings.meter_id` | column | **`electric_meter.id`** | Declared `FOREIGN KEY … REFERENCES electric_meter(id)`. |
| `time_sheet.meter_id` | column | **`electric_meter.id`** | Same. |
| `ReadElectricMeter.device_id` | DTO field | **`address`** | Despite the name. Goes straight to `client.read_input_registers(device_id=…)`. |
| `ElectricMeterEnergyReadingDTO.meter_id` | DTO field | **ambiguous — depends on producer** | `read_energy()` sets it to `input.device_id`, i.e. the *Modbus address*. The entity read back from the DB holds the true *PK*. Never trust this field without knowing who built the object. |
| `GET /voltage/{meter_address}` | path param | **`address`** | |
| `GET /electric/{meter_id}}` | path param | **`id`** (nominally) | Endpoint is broken; see §8. |
| `PUT /meters/{meter_id}` | path param | **`id`** | |
| Bruno `meterAddress` / `meterId` env vars | | address / id respectively | Both set to `1`. |

### The correct translation path

`app/repository/electric_meter_repository.py:74` — `get_electric_meter_id_by_address(conn, address)`
resolves address → PK (`ORDER BY id ASC`, returns the first match, `None` if unregistered). The
scheduler job uses it correctly (`service/timesheet/time_sheet_service.py:100`) and refuses to
insert a reading when it returns `None`.

**[AS-IS DEFECT]** `app/service/electric_service.py:96` does **not**:

```python
found_energy = get_energy_readings_by_meter_id(conn, meter.address)   # should be meter.id
```

Every energy value served by `GET /electric/all` is therefore looked up by Modbus address against a
primary-key column. It returns the right rows today only because `id == address` in this
deployment. A server that ingests `/electric/all` is ingesting data that silently misattributes
readings the moment any meter is added, removed, or re-addressed.

**Server rule:** the server's own meter identity must be a tuple of
`(device/site identity, electric_meter.id)` — and the device identity has to be supplied out of
band, because nothing in any current payload carries it. Do not treat `address` as an identity; it
is a bus-topology detail and is reused across sites.

---

## 4. Relational data model (SQLite)

Schema source: `app/resources/scripts/create.sql`. Verified against the live database. No indexes
beyond the implicit primary keys. No `UNIQUE` constraints anywhere. `PRAGMA foreign_keys = ON` is
issued per-connection by most repository functions, but **not** by the plain `sqlite3.connect()`
calls in the router read paths — so FK enforcement is inconsistent.

SQLite type affinities are as declared; `BOOLEAN` and `INTEGER` both store 0/1 integers, and `TEXT`
columns in this schema store dates in several different formats (see §14).

### 4.1 `electric_meter` — the registered meter

| Column | Type | Null | Default | Semantics |
|---|---|---|---|---|
| `id` | `INTEGER` | PK | autoincrement | Local primary key. FK target. |
| `name` | `TEXT` | NOT NULL | — | Human label. Free text, **not unique**, may be non-ASCII (Cyrillic in `insert.sql`), may contain `#` and spaces (`Tarfo#1-7`). Used as the Excel "Meter Name" and as the Modbus read `label`. |
| `address` | `INTEGER` | NOT NULL | — | Modbus RTU slave address. Not unique at the schema level. |
| `meter_config` | `INTEGER` | NOT NULL | — | FK → `electric_meter_config(id)`, `ON DELETE CASCADE`. No default — see the insert defect in §4.1.1. |
| `active` | `BOOLEAN` | NOT NULL | `TRUE` | 1/0. **Read by nothing in the backend** — the scheduler filters on `time_sheet.is_active`, not on this. |
| `created_on` | `TEXT` | nullable | — | Mixed formats, see §14.1. |

#### 4.1.1 Two insert paths, two different shapes **[AS-IS DEFECT]**

| Path | Supplies `meter_config`? | Works? |
|---|---|---|
| `POST /meters` (`router/meter_routs.py`) | yes, required in the request body | **yes** — correct parameterised SQL, idempotent on `(name, address)`, 201 + the row |
| `POST /meter` (`router/electric_energy_routs.py` → `service/electric_service.create_electric_meter`) | no | **never** — infinite recursion, then a `NOT NULL` violation on `meter_config`, then a broken lookup query |

`repository/electric_meter_repository.insert_electric_meter` builds its INSERT from
`dataclasses.asdict(ElectricMeterEntity)`, and that dataclass has no `meter_config` field at all, so
it can only ever violate the `NOT NULL` constraint.

**Server rule:** treat `POST /meters` as the only meter-creation contract. `POST /meter` and its
`CreateElectricMeter` body shape are dead; do not build against them.

### 4.2 `electric_meter_config` — meter make/model

| Column | Type | Null | Semantics |
|---|---|---|---|
| `id` | `INTEGER` | PK | |
| `name` | `TEXT` | NOT NULL | Config family name, e.g. `Sentron` |
| `make` | `TEXT` | NOT NULL | Manufacturer. Current value is misspelled: **`"Simens"`** |
| `model` | `TEXT` | NOT NULL | e.g. `PAC3120` |

Joined into `get_all_electric_meters()` as an `INNER JOIN` — **a meter whose `meter_config` does not
resolve is invisible to `GET /electric/all`, `GET /all/totenergy` and the scheduler**, silently.

### 4.3 `electric_meter_read_config` — the register map

| Column | Type | Null | Semantics |
|---|---|---|---|
| `id` | `INTEGER` | PK | |
| `name` | `TEXT` | NOT NULL | e.g. `Sentron PAC3120 Read Energy`. Note the trailing space in `'Sentron PAC3120 Read Current '` — it is part of the stored value and the seed script's `WHERE NOT EXISTS` guard depends on it. |
| `description` | `TEXT` | NOT NULL | Free text. |
| `address_from` | `TEXT` | NOT NULL | First Modbus register. **Stored as TEXT** holding a decimal integer. |
| `address_to` | `TEXT` | NOT NULL | Last Modbus register, inclusive. **Stored as TEXT.** |
| `version` | `INTEGER` | NOT NULL | Always `1`. Unused by code. |
| `register_length` | `INTEGER` | NOT NULL | Registers per value: `2` → 32-bit float, `4` → 64-bit double. |
| `created_on` | `TEXT` | nullable | `'22.01.2026'` — **dd.mm.yyyy**, not ISO. Never parsed. |

**[AS-IS DEFECT]** `repository/electric_meter_config_repository.py:28` filters
`WHERE address_to = 440`, with a `# TODO this is temporary hard coded read for sentron(Markely)`
comment. Any read config not ending exactly at register 440 is invisible to the scheduler. The
comparison is also an integer literal against a `TEXT` column, relying on SQLite type affinity.

**[AS-IS DEFECT]** The register range stored in the database for energy is **401..440**, but every
code path that actually reads energy hardcodes **801..840** and discards the DB value — see §6.3.
The database register map is, in practice, decorative.

### 4.4 `mm_meter_read_config_meter_read_config` — config ↔ read-config join

| Column | Type | Semantics |
|---|---|---|
| `id` | `INTEGER` | PK |
| `electric_meter_config` | `INTEGER` NOT NULL | FK → `electric_meter_config(id)`, cascade |
| `electric_meter_read_config` | `INTEGER` NOT NULL | FK → `electric_meter_read_config(id)`, cascade |

Many-to-many. The table name repeats `meter_read_config` twice; the columns do not. Keep the name
literal.

### 4.5 `electric_meter_energy_readings` — the time series **(the payload that matters)**

This is the table the server exists to collect.

| Column | Type | Null | Semantics |
|---|---|---|---|
| `id` | `INTEGER` | PK | |
| `meter_id` | `INTEGER` | NOT NULL | FK → **`electric_meter(id)`**, cascade |
| `TotalActiveEnergyImportTariff1` | `REAL` | NOT NULL | 64-bit double, decoded from registers 801–804 |
| `TotalActiveEnergyImportTariff2` | `REAL` | NOT NULL | registers 805–808 |
| `TotalActiveEnergyExportTariff1` | `REAL` | NOT NULL | registers 809–812 |
| `TotalActiveEnergyExportTariff2` | `REAL` | NOT NULL | registers 813–816 |
| `TotalReactiveEnergyImportTariff1` | `REAL` | NOT NULL | registers 817–820 |
| `TotalReactiveEnergyImportTariff2` | `REAL` | NOT NULL | registers 821–824 |
| `TotalReactiveEnergyExportTariff1` | `REAL` | NOT NULL | registers 825–828 |
| `TotalReactiveEnergyExportTariff2` | `REAL` | NOT NULL | registers 829–832 |
| `TotalApparentEnergyTariff1` | `REAL` | NOT NULL | registers 833–836 |
| `TotalApparentEnergyTariff2` | `REAL` | NOT NULL | registers 837–840 |
| `created_on` | `TEXT` | NOT NULL | UTC ISO-8601 **with** `+00:00` offset — see §14.1 |

**Column names are PascalCase in the database.** This is unusual for SQLite and is preserved
verbatim by the entity and DTO classes. Do not snake_case them without a deliberate mapping layer.

Semantics of the values:

- These are **cumulative totalisers read off the meter**, not deltas or interval consumption. They
  increase monotonically while the meter runs, and reset only if the physical meter is reset or
  replaced. Consumption over a window is `last − first` in that window.
- **Tariff 1 / Tariff 2** are the meter's two tariff registers (day/night or peak/off-peak as
  configured on the device itself). The edge does not know which is which.
- **Import** = energy drawn from the grid; **Export** = energy fed back.
- The **unit is whatever the PAC3120 register holds** — the edge never scales the value. The HTTP
  layer labels these inconsistently as `Wh`, `kWh`, `KWh`, `varh` and `VAh` depending on which
  endpoint you ask (§14.2). **The stored REAL is the ground truth and it is unitless.**
- There is **no row-level quality/status flag**, no "partial read" marker, and no raw-register
  snapshot. A reading is either present and complete (all ten columns) or absent.
- There is **no uniqueness constraint** on `(meter_id, created_on)`. The scheduler's immediate
  "control read" on every startup or every `POST /time/sheet/scheduler/start` call inserts an extra
  row outside the regular cadence, so **duplicate-looking rows a few seconds apart are normal and
  expected**.

Current row count: **0**. The table has never been populated in this checkout (no hardware
attached). The server cannot rely on sample data existing.

### 4.6 `time_sheet` — the polling schedule

| Column | Type | Null | Default | Semantics |
|---|---|---|---|---|
| `id` | `INTEGER` | PK | | |
| `meter_id` | `INTEGER` | NOT NULL | | FK → `electric_meter(id)`, cascade. One row = one schedule for one meter. |
| `name` | `TEXT` | NOT NULL | | Label, e.g. `Default 60-min polling`. Not unique. |
| `is_active` | `INTEGER` | NOT NULL | `1` | 1/0. **This is the flag the scheduler honours.** |
| `interval` | `INTEGER` | NOT NULL | | **Minutes.** Passed to `schedule.every(interval).minutes`. |
| `created_on` | `TEXT` | NOT NULL | | Mixed formats, see §14.1. |

Multiple time sheets per meter are possible and would register multiple independent jobs — except
that `activate_time_sheets_service_con` builds a `{address: time_sheet}` dict, so **only one time
sheet per Modbus address survives**, and which one wins depends on `ORDER BY ts.created_on DESC`
row order. Note `interval` is a reserved-ish word in some SQL dialects; it is unquoted here and
works in SQLite.

### 4.7 `error_log` — device-side error journal

| Column | Type | Null | Default | Semantics |
|---|---|---|---|---|
| `id` | `INTEGER` | PK | | |
| `error_message` | `TEXT` | NOT NULL | | Free-text `str(exception)`. |
| `status` | `TEXT` | NOT NULL | `'new'` | Only two values are ever written: `'new'` (the default) and `'critical'` (scheduler startup failure, `main.py:42`). No state machine, nothing ever updates it. |
| `created_on` | `TEXT` | NOT NULL | | UTC ISO-8601 with offset. |

Written only by `repository/error_log_repository.log_error()`, which is called from exactly one
place (`main.py:42`). **No HTTP endpoint exposes this table.** Most errors in the system are
`print()`ed to stdout and never persisted. Current row count: 0.

This table is the obvious basis for device health reporting to the server, but today it is
near-empty by construction.

### 4.8 `meter_group`, `mm_meter_group_electric_meter` — reserved, unused

Both exist in the schema, both are empty, and **no Python or Java code reads or writes either**.
`meter_group` carries `meter_id` (oddly, given the join table also does), `description`, and
`created_on`. `GET /electric/all` emits a hardcoded `"groupId": "none"` string instead of consulting
them. Treat grouping as an unimplemented feature with a reserved schema.

### 4.9 Entity-relationship summary

```
electric_meter_config 1 ──< mm_meter_read_config_meter_read_config >── 1 electric_meter_read_config
        │ 1                                                                 (register map: from/to/length)
        │
        │ N
  electric_meter ──1──< time_sheet                 (polling schedule, interval in minutes)
   (id, address)   ──1──< electric_meter_energy_readings   (cumulative totalisers + created_on)
                   ──1──< mm_meter_group_electric_meter >── meter_group   [unused]

  error_log   (standalone, no FK)
```

---

## 5. Current data inventory

Live contents of `app/repository/meter.db` as of 2026-10-06. This is the **Markeli** client
deployment, seeded by `app/resources/scripts/clients/markely_insert.sql`.

### 5.1 Row counts

| Table | Rows |
|---|---|
| `electric_meter` | 10 |
| `electric_meter_config` | 1 |
| `electric_meter_read_config` | 3 |
| `mm_meter_read_config_meter_read_config` | 3 |
| `time_sheet` | 10 |
| `electric_meter_energy_readings` | **0** |
| `error_log` | 0 |
| `meter_group` | 0 |
| `mm_meter_group_electric_meter` | 0 |

### 5.2 `electric_meter`

| id | name | address | meter_config | active | created_on |
|---|---|---|---|---|---|
| 1 | `TBA 8` | 1 | 1 | 1 | `2026-09-29 04:34:15` |
| 2 | `Ampak` | 2 | 1 | 1 | `2026-09-29 04:34:15` |
| 3 | `Ledena Voda` | 3 | 1 | 1 | `2026-09-29 04:34:15` |
| 4 | `Hladilnici` | 4 | 1 | 1 | `2026-09-29 04:34:16` |
| 5 | `Kompresor` | 5 | 1 | 1 | `2026-09-29 04:34:16` |
| 6 | `Priemno` | 6 | 1 | 1 | `2026-09-29 04:34:16` |
| 7 | `Tarfo#1-7` | 7 | 1 | 1 | `2026-09-29 04:34:16` |
| 8 | `Homo OHT` | 8 | 1 | 1 | `2026-09-29 04:34:17` |
| 9 | `Priem KM` | 9 | 1 | 1 | `2026-09-29 04:34:17` |
| 10 | `Priem UHT` | 10 | 1 | 1 | `2026-09-29 04:34:17` |

### 5.3 `electric_meter_config`

| id | name | make | model |
|---|---|---|---|
| 1 | `Sentron` | `Simens` *(sic)* | `PAC3120` |

### 5.4 `electric_meter_read_config`

| id | name | address_from | address_to | version | register_length | created_on |
|---|---|---|---|---|---|---|
| 1 | `Sentron PAC3120 Read Voltage` | `1` | `6` | 1 | 2 | `22.01.2026` |
| 2 | `Sentron PAC3120 Read Current ` | `13` | `18` | 1 | 2 | `22.01.2026` |
| 3 | `Sentron PAC3120 Read Energy` | `401` | `440` | 1 | 4 | `22.01.2026` |

`mm_meter_read_config_meter_read_config` links config 1 to all three read configs (rows 1→1, 1→2,
1→3).

### 5.5 `time_sheet`

Ten rows, `id` 1..10 mapping 1:1 to `meter_id` 1..10, all identical apart from the FK:

| field | value |
|---|---|
| `name` | `Default 60-min polling` |
| `is_active` | 1 |
| `interval` | 60 (minutes) |
| `created_on` | `2026-09-29 04:34:15` … `04:34:17` |

### 5.6 Other client seed sets

`app/resources/scripts/insert.sql` (the older generic seed, **not** run at startup) defines a
different 12-meter set with Cyrillic names — `MKP`, `UHT`, `Priemno`, `Ледена вода`,
`Хладилни камери`, `Компресори`, `Други`, `Парен котел`, `Приемно`, `UHT 2`, `МКП`, `Отопление`,
addresses 1..12. A stale copy of that set survives in `app/repository/meter1.db` (12 meters, 12 time
sheets). `app/resources/scripts/clients/insert_lucuvid.sql` is a third client set.

**Server implication:** meter names are client-specific free text, frequently non-ASCII, and not
stable identifiers. The same logical site can be re-seeded with different names. Persist names as
UTF-8 display labels only.

---

## 6. Modbus acquisition layer

**[PROTECTED] — none of this may be changed.** The server must accept these semantics as given.

### 6.1 Transport

`pymodbus` `ModbusSerialClient` over RS-485, Modbus RTU framing. All reads use **input registers**
(function code 4) via `client.read_input_registers(address=…, count=…, device_id=…)`.

Serial parameters come from `settings.json → Modbuss_RTU` (current repo value: `COM2`, 9600 baud,
parity `N`, 1 stop bit, 8 data bits, 1 s timeout).

**[AS-IS DEFECT]** Only `read_anything()` and `read_energy()` honour the configured client. The
functions `read_voltage`, `read_voltage1`, `read_amperage` and `read_total_energies` each construct
their **own** client with hardcoded `port='COM3', baudrate=9600, parity='N', stopbits=1, bytesize=8,
timeout=1`, ignoring `settings.json` entirely — even across a process restart. The repo currently
configures `COM2`, so the voltage/amperage paths and the configured path point at different ports.

A client is opened and `close()`d per call. The one shared client built at import time
(`router/constants.SETTINGS_CLIENT`) is **not thread-safe** and is used concurrently by HTTP
handlers and the scheduler thread.

### 6.2 Decoders

`app/reading/reading_logic.py`:

| Function | Registers | Decoding |
|---|---|---|
| `decode_int_len2(registers, i)` | 2 | `(r[i] << 16) \| r[i+1]` → `struct.unpack('>f', pack('>I', …))` → **big-endian 32-bit IEEE-754 float** |
| `decode_double_len4(registers, i)` | 4 | `(r[i]<<48)\|(r[i+1]<<32)\|(r[i+2]<<16)\|r[i+3]` → `struct.unpack('>d', pack('>Q', …))` → **big-endian 64-bit IEEE-754 double** |

Register order is big-endian word order (most significant register first), no word swap. The
in-code comment records that an earlier version mistakenly used `decode_int_len2` for 4-register
values, dropping half the bytes and corrupting every energy reading — that bug is fixed and must not
be reintroduced.

### 6.3 Register layouts actually used

Two different energy layouts coexist. **This matters: only one of them produces the persisted data
the server will consume.**

#### A. Persisted layout — `read_energy()`, uniform stride 4 — **AUTHORITATIVE**

Block `801..840` (40 registers), `count = end − start + 1 = 40`, `length = 4`, decoded with
`decode_double_len4` at `range(0, 40, 4)` → indices `0,4,8,12,16,20,24,28,32,36`:

| # | Register offset | Absolute registers | Maps to |
|---|---|---|---|
| 0 | 0 | 801–804 | `TotalActiveEnergyImportTariff1` |
| 1 | 4 | 805–808 | `TotalActiveEnergyImportTariff2` |
| 2 | 8 | 809–812 | `TotalActiveEnergyExportTariff1` |
| 3 | 12 | 813–816 | `TotalActiveEnergyExportTariff2` |
| 4 | 16 | 817–820 | `TotalReactiveEnergyImportTariff1` |
| 5 | 20 | 821–824 | `TotalReactiveEnergyImportTariff2` |
| 6 | 24 | 825–828 | `TotalReactiveEnergyExportTariff1` |
| 7 | 28 | 829–832 | `TotalReactiveEnergyExportTariff2` |
| 8 | 32 | 833–836 | `TotalApparentEnergyTariff1` |
| 9 | 36 | 837–840 | `TotalApparentEnergyTariff2` |

`read_energy()` returns an `ElectricMeterEnergyReadingDTO` with `meter_id = input.device_id`
(i.e. the **Modbus address**, not the PK — see §3). This is the only function that writes to
`electric_meter_energy_readings`, via the scheduler.

#### B. Live-read-only layout — irregular indices `[0, 3, 7, 11, 15]`

Used by `read_total_energies()` and by `GET /totenergy` / `GET /totenergy/import` /
`GET /all/totenergy`, with the in-code comment "matches known-good `read_total_energies` layout".
Yields **5** values from offsets 0, 3, 7, 11, 15 — *not* multiples of 4, so this layout does not
agree with layout A. It never reaches the database.

**Server rule:** model stored readings on layout A. Treat `/totenergy*` output as a diagnostic live
probe whose field mapping disagrees with the persisted data, and do not reconcile the two.

#### C. Voltage and amperage

| Function | Call | Decode | Returns |
|---|---|---|---|
| `read_voltage(mater_id)` | `address=mater_id`, `count=6`, **no `device_id`** | `decode_int_len2` at offsets 0, 2, 4 | `[l1, l2, l3]` list of float |
| `read_voltage1(meter_id)` | `read_anything`, `offset_address=[1,6]`, `device_id=meter_id`, `length=2` | `decode_int_len2` at `range(0,6,2)` | `[l1, l2, l3]` |
| `read_amperage(mater_id)` | `address=mater_id`, `count=6`, **no `device_id`** | `decode_int_len2` at offsets 0, 2, 4 | `ThreePhaseData("Amperage", l1, l2, l3, "A")` |

**[AS-IS DEFECT, PROTECTED]** `read_voltage` and `read_amperage` pass the **meter address as the
starting register address** and never set `device_id`. So "voltage for meter 3" reads registers
3..8 from the bus's default device, and amperage reads the same registers as voltage rather than the
configured 13..18. `GET /voltage` and `GET /voltage/{meter_address}` call `read_voltage`;
`GET /electric/all` calls `read_voltage1` (the correct one) plus `read_amperage` (the incorrect
one).

**Server rule:** voltage and amperage values from this device are **not trustworthy as per-phase
measurements of a named meter**. Ingest them, if at all, as diagnostic telemetry — never as billing
or analytic inputs. Only the persisted energy totalisers are sound.

#### D. Failure behaviour

- Connect failure → `Exception("Failed to connect to Modbus device")`
- Modbus exception response → `Exception(str(result))`, e.g.
  `Modbus Error: [Input/Output] No response received after 3 retries, continue with next request`
- No retry, no backoff, no circuit breaker. With no hardware attached, every live read blocks for
  the 1 s serial timeout and then raises.

**[AS-IS DEFECT]** `service/electric_service.py:103` tries to short-circuit repeated failures, but
matches the retry message by **exact string equality** only. The no-hardware message
(`Failed to connect to Modbus device`) never matches, so `GET /electric/all` pays the full timeout
for every meter, every call.

---

## 7. In-process type definitions (DTOs and entities)

Field names here are the **JSON key names** FastAPI emits, because the endpoints return these
objects directly through `jsonable_encoder`. All are plain `@dataclass`, not Pydantic (except the
three Pydantic models in `meter_routs.py`).

### 7.1 `app/domain/energy_structs.py`

```python
@dataclass
class ElectricMeterEntityDTO:
    name: str
    address: int                     # Modbus slave address

@dataclass
class ThreePhaseData:
    name: str                        # "Voltage" | "Amperage"
    l1: float
    l2: float
    l3: float
    unit: str                        # "V" | "A"

@dataclass
class ElectricMeterEnergyReadingDTO:
    meter_id: int                    # AMBIGUOUS: PK or Modbus address, per producer (§3)
    TotalActiveEnergyImportTariff1: float
    TotalActiveEnergyImportTariff2: float
    TotalActiveEnergyExportTariff1: float
    TotalActiveEnergyExportTariff2: float
    TotalReactiveEnergyImportTariff1: float
    TotalReactiveEnergyImportTariff2: float
    TotalReactiveEnergyExportTariff1: float
    TotalReactiveEnergyExportTariff2: float
    TotalApparentEnergyTariff1: float
    TotalApparentEnergyTariff2: float
    # NOTE: no timestamp. created_on exists only on the entity/DB row.

@dataclass
class ReadElectricMeter:             # internal read descriptor, never serialised
    device_id: int                   # Modbus slave address
    length: int                      # 2 or 4 registers per value
    label: str                       # meter name, used in the Excel export
    offset_address: list             # [first_register, last_register] inclusive
    unit: str
    action: any                      # the decoder callable
    client: any                      # ModbusSerialClient
    indices: list = None             # explicit decode offsets; overrides the stride loop

@dataclass
class ReadElectricMeterRequest:      # HTTP request body for /anything/len2 and /anything/len4
    device_id: int
    label: str
    offset_address: list[int]
    unit: str

@dataclass
class ReadElectricMeterWithData:     # internal aggregate
    electric_meter: ElectricMeterEntityDTO
    energy: ElectricMeterEnergyReadingDTO     # may be None
    voltage: ThreePhaseData                   # may be None
    amperage: ThreePhaseData                  # may be None
```

### 7.2 `app/domain/energy_request_structs.py`

```python
@dataclass
class CreateElectricMeter:           # body of the dead POST /meter
    electric_meter_read_config_name: str
    electric_meter_name: str
    electric_meter_description: str  # accepted and then discarded — no column for it
    electric_meter_address: int      # int
    time_sheet_name: str
    time_sheet_interval: str         # STRING, cast with int() later
```

### 7.3 `app/domain/time_sheet_structs.py`

```python
@dataclass
class TimeSheetDTO:
    meter_id: int
    name: str
    is_active: bool
    interval: int                    # minutes
```

Used only by the three dead scheduler variants.

### 7.4 `app/domain/meter_config_structs.py` — defined, never used

```python
@dataclass
class ElectricMeterConfigDTO:
    name: str
    make: str
    model: str

@dataclass
class MeterReadConfigDTO:
    name: str
    description: str
```

Neither is referenced anywhere. **No endpoint exposes the meter config or register map.** If the
server needs the register map, it has to read SQLite directly or a new endpoint has to be added.

### 7.5 `app/repository/entity/electric_entity_structures.py`

```python
@dataclass
class ElectricMeterEntity:
    id: int
    name: str
    address: int
    created_on: datetime.datetime    # → ISO-8601 string in JSON
    # NOTE: no meter_config, no active — the dataclass is narrower than the table.

@dataclass
class ElectricMeterEnergyReadingEntity:
    id: int
    meter_id: int                    # electric_meter.id (true PK)
    TotalActiveEnergyImportTariff1: float
    # … all ten totalisers, same names as the columns …
    created_on: datetime.datetime
    def to_dto(self) -> ElectricMeterEnergyReadingDTO:   # drops id and created_on
        ...
```

`to_dto()` **discards the timestamp**, which is why `GET /electric/all` has to substitute
`str(datetime.now())` as `lastUpdated` — the time the row was *read*, not when it was *recorded*.

### 7.6 `app/repository/entity/time_sheet_entity_scructures.py`

(filename misspelling is literal)

```python
@dataclass
class TimeSheetEntity:
    id: int
    name: str
    interval: int                    # minutes
    is_active: bool
    meter: ElectricMeterEntity       # nested object
    created_at: datetime.datetime    # NOTE: created_at here, created_on in the DB column
```

### 7.7 `app/domain/settings_struct.py`

```python
@dataclass
class Modbuss_RTU_Settings:          # two s's — literal
    port: str; baudrate: int; parity: str; stopbits: int; bytesize: int; timeout: int

@dataclass
class Remote_Database_Settings:
    url: str; user: str; password: str; db_name: str      # loaded, served, never used

@dataclass
class Local_Database_Settings:
    db_name: str                                          # loaded, served, never used

@dataclass
class Settings:
    Modbuss_RTU: Modbuss_RTU_Settings
    Remote_Database: Remote_Database_Settings
    Local_Database: Local_Database_Settings
```

`Settings.to_json()` serialises with `indent=4, sort_keys=True`, so the on-disk file has
alphabetically ordered keys while the HTTP response has declaration order.

---

## 8. HTTP API contract

Base URL: `http://<host>:8000`. No prefix except where noted. **No authentication on any
endpoint.** Content type `application/json` throughout except the two HTML doc pages.

### 8.1 Route table

| # | Method | Path | Router | Hardware | DB | Status |
|---|---|---|---|---|---|---|
| 1 | GET | `/` | `main.py` | no | no | works |
| 2 | GET | `/voltage` | `electric_energy_routs.py` | yes | no | works with hardware; hardcoded `COM3`, address 1 |
| 3 | GET | `/voltage/{meter_address}` | same | yes | no | works with hardware; see §6.3C defect |
| 4 | GET | `/electric/all` | same | partial | yes | works; degrades to nulls without hardware |
| 5 | GET | `/electric/{meter_id}}` | same | — | — | **broken** — literal `}}` in the path *and* returns `null` |
| 6 | GET | `/totenergy` | same | yes | no | works with hardware |
| 7 | GET | `/totenergy/import` | same | yes | no | works with hardware |
| 8 | GET | `/all/totenergy` | same | yes | yes | **always 500** — index overflow, see §8.8 |
| 9 | POST | `/meter` | same | no | yes | **always 500** — recursion |
| 10 | POST | `/anything/len4` | same | yes | no | works with hardware |
| 11 | POST | `/anything/len2` | same | yes | no | works with hardware |
| 12 | POST | `/meters` | `meter_routs.py` | no | yes | **works — the reference implementation** |
| 13 | PUT | `/meters/{meter_id}` | same | no | yes | **works** |
| 14 | GET | `/settings/` | `settings_routs.py` | no | no | works; trailing slash required |
| 15 | POST | `/settings/` | same | no | no | works; does not take effect until restart |
| 16 | GET/POST | `/settings/graphql` | `settings_graph_routs.py` | no | no | works; placeholder schema |
| 17 | GET | `/time/sheet` | `time_sheets_routs.py` | no | yes | works |
| 18 | POST | `/time/sheet/scheduler/start` | same | on first run | yes | works |
| 19 | GET | `/openapi.json`, `/docs`, `/redoc` | FastAPI | no | no | works |

There is **no** endpoint for: listing meters (only `/electric/all`, which also does live reads),
deleting a meter, reading raw energy-reading history, listing/creating/updating/deleting time sheets,
reading the register map, or reading `error_log`.

**[AS-IS DEFECT]** `bruno/README.md` lists `GET /totenergy/{meter_address}`. That route does not
exist — the actual route is the literal path `/totenergy/import`, so `GET /totenergy/1` returns
**404**. The Bruno request "Get total energy by meter address" is stale.

### 8.2 `GET /` — liveness

Response `200`:

```json
{ "message": "Hello Bigger Applications!" }
```

The only non-destructive, hardware-independent liveness probe. `edge-monitoring`'s `status` command
uses it.

### 8.3 `GET /voltage` and `GET /voltage/{meter_address}`

`meter_address`: `int` path parameter (Modbus address). The unparameterised form hardcodes `1`.

Response `200`:

```json
{ "name": "Voltage", "l1": 0.0, "l2": 0.0, "l3": 0.0, "unit": "V" }
```

`l1`/`l2`/`l3` are 32-bit-float-decoded doubles. See §6.3C — these values are read from the wrong
registers. `500` on any Modbus failure.

### 8.4 `GET /electric/all` — the aggregate dashboard payload

The richest endpoint and the one a server is most likely to poll. Response is a **JSON array**, one
object per row returned by `get_all_electric_meters()` (i.e. per meter with a resolvable
`meter_config`), ordered by `electric_meter.created_on DESC`.

Per-element shape — **every field is listed; "literal" means the value is hardcoded in
`service/electric_service.py` and carries no information**:

| Key | Type | Source / value |
|---|---|---|
| `id` | **string** | `f"{name} Sentron"`, e.g. `"TBA 8 Sentron"`. **Not** the numeric PK. Hardcodes the model family. |
| `name` | string | `electric_meter.name` |
| `location` | string | literal `" Locations"` (leading space) |
| `status` | string | literal `"active"` |
| `groupId` | string | literal `"none"` |
| `lastReading` | number \| **string** | latest `TotalApparentEnergyTariff1`, or the **string** `"0"` when no readings exist |
| `lastUpdated` | string | `str(datetime.now())` → `"2026-10-06 07:12:34.567890"` — **local time, space-separated, not ISO, no offset**. The time of the HTTP call, *not* of the reading. |
| `dailyAverage` | number | literal `0` (TODO in code) |
| `peakLoad` | string | literal `"0"` (TODO) |
| `peakTime` | string | same value as `lastUpdated` (TODO) |
| `monthlyCost` | number | literal `0` (TODO) |
| `rate` | number | literal `0` (TODO) |
| `type` | string | literal `"Modbus Meter"` |
| `voltage` | string | literal `"690 V AC (L-L) / 400 V AC (L-N)"` — a nameplate string, not a measurement |
| `phase` | number | literal `3` |
| `installDate` | string | literal `" "` (a single space) |
| `manufacturer` | string | literal `"Simens"` *(sic)* |
| `alerts` | array | always exactly one placeholder: `[{ "message": " ", "date": <lastUpdated>, "severity": "low" }]` |
| `voltageData` | object | `{name, l1, l2, l3, unit}` — from `read_voltage1`; `{"", 0, 0, 0, ""}` when the read failed |
| `amperageData` | object | `{name, l1, l2, l3, unit}` — from `read_amperage`; same zero-fill fallback |
| `energyData` | object | 20 keys: ten camelCase totalisers + ten `…Unit` strings (see below) |

`energyData` keys, in order — values are numbers when a reading exists and the **string `"0"`**
when it does not:

```
totalActiveEnergyImportTariff1       totalActiveEnergyImportTariff1Unit   = "kWh"
totalActiveEnergyImportTariff2       totalActiveEnergyImportTariff2Unit   = "kWh"
totalActiveEnergyExportTariff1       totalActiveEnergyExportTariff1Unit   = "kWh"
totalActiveEnergyExportTariff2       totalActiveEnergyExportTariff2Unit   = "kWh"
totalReactiveEnergyImportTariff1     totalReactiveEnergyImportTariff1Unit = "varh"
totalReactiveEnergyImportTariff2     totalReactiveEnergyImportTariff2Unit = "varh"
totalReactiveEnergyExportTariff1     totalReactiveEnergyExportTariff1Unit = "varh"
totalReactiveEnergyExportTariff2     totalReactiveEnergyExportTariff2Unit = "varh"
totalApparentEnergyTariff1           totalApparentEnergyTariff1Unit       = "VAh"
totalApparentEnergyTariff2           totalApparentEnergyTariff2Unit       = "VAh"
```

Note the casing change: the **database and DTO use PascalCase** (`TotalActiveEnergy…`), this
endpoint emits **camelCase** (`totalActiveEnergy…`). Both spellings are live in the system.

Only the **single most recent** reading per meter is included (`found_energy[0]` after
`ORDER BY created_on DESC`). There is no history, no pagination, no `since` filter, and no way to
ask for anything other than "latest".

Performance: with no hardware, each element costs two serial timeouts (~2 s), serially. Ten meters
≈ 20 s per call. The short-circuit that should prevent this is broken (§6.3D).

**Server verdict:** this payload is shaped for a specific web dashboard, mixes measurements with
hardcoded placeholders, unions numbers with strings, carries a non-ISO local timestamp that is not
the reading time, and cannot express history. **It is unsuitable as a data-ingestion contract.**
Use it only to drive a UI; collect readings another way (§15).

### 8.5 `GET /electric/{meter_id}}` — broken, do not use

`router/electric_energy_routs.py:55` declares `@router.get("/electric/{meter_id}}")` with a **stray
extra `}`**, so the registered path template literally ends in `}`. The URL that matches is
`/electric/1}`. Even when matched, the service function
(`electric_service.get_single_meter_with_data`) is a stub that `return None`, so the response is
always `null`. Both defects must be fixed before this path means anything.

### 8.6 `GET /totenergy`

Live read of registers 801..840, Modbus address **hardcoded to 1**, layout B (§6.3). Response `200`
— five values and five unit labels:

```json
{
  "Total_active_energy_import_tariff_1": 0.0,
  "Total_active_energy_import_tariff_2": 0.0,
  "Total_active_energy_export_tariff_1": 0.0,
  "Total_active_energy_export_tariff_2": 0.0,
  "Total_reactive_energy_import_tariff_1": 0.0,
  "Total_active_energy_import_tariff_1_unit": "Wh",
  "Total_active_energy_import_tariff_2_unit": "Wh",
  "Total_active_energy_export_tariff_1_unit": "Wh",
  "Total_active_energy_export_tariff_2_unit": "Wh",
  "Total_reactive_energy_import_tariff_1_unit": "varh"
}
```

Third naming convention for the same quantities: `Snake_Case_With_Capitals`. Note the request
descriptor sets `unit="KWh"` internally while the response says `"Wh"`.

### 8.7 `GET /totenergy/import`

A **literal path segment** `import` — not a parameter. Registers 801..804, `indices=[0]`, address 1.

```json
{ "Total_active_energy_import": 0.0, "Total_active_energy_import_tariff_1_unit": "Wh" }
```

### 8.8 `GET /all/totenergy` — always fails

Intended: the `/totenergy` payload per meter, as an array. Two defects:

1. **[AS-IS DEFECT]** The loop over meters never uses the loop variable: every iteration reads the
   same hardcoded `offset_address=[801, 840], device_id=1`. Every "different meter" in the result
   would be the same register read repeated N times.
2. **[AS-IS DEFECT]** It passes `indices=[0, 3, 7, 11, 15]` (5 values) but the response builder
   indexes `tot_energy[0]` … `tot_energy[9]` (10 values) → `IndexError` → caught → **HTTP 500 on
   every call, even with working hardware**.

Do not build against this endpoint.

### 8.9 `POST /anything/len4` and `POST /anything/len2` — generic register reader

Request body — `ReadElectricMeterRequest`, all fields required:

```json
{ "device_id": 1, "label": "Tot_energy", "offset_address": [801, 840], "unit": "KWh" }
```

- `device_id` — Modbus slave address
- `offset_address` — `[first, last]`, **inclusive**; `count = last − first + 1`. Only the first and
  last elements are used (`offset_address[0]` and `[-1]`).
- `label`, `unit` — echoed nowhere in `len4`; purely decorative here.
- `length` and the decoder are fixed server-side: `len4` → 4 registers / `decode_double_len4`,
  `len2` → 2 registers / `decode_int_len2`. No `indices`, so the uniform stride loop is used.

`len4` response: the same 20-key object as §8.4's hardcoded energy shape, indexing
`tot_energy[0..9]`. **A range yielding fewer than 10 doubles (i.e. fewer than 40 registers) returns
500.**

`len2` response — the only endpoint that returns a raw array:

```json
{ "tot_energy": [0.0, 0.0, 0.0], "unit": "Wh" }
```

Array length is `ceil(count / 2)`; the `"unit"` is hardcoded `"Wh"` regardless of what you asked
for.

These two are effectively a **raw Modbus passthrough over HTTP, unauthenticated**. Anyone who can
reach port 8000 can read any input register from any slave on the bus.

### 8.10 `POST /meter` — dead, do not use

Body is `CreateElectricMeter` (§7.2). Always returns `500` (`RecursionError`), and would fail on
three further bugs if that one were fixed. Superseded by `POST /meters`.

### 8.11 `POST /meters` — create meter (**the reference contract**)

Prefix `/meters`, path `""` → the exact URL is `/meters` (no trailing slash, no redirect).
OpenAPI tag `meters`.

Request (`CreateMeterRequest`, Pydantic — all fields required):

```json
{ "name": "Meter A", "address": 1, "meter_config": 1 }
```

| Field | Type | Notes |
|---|---|---|
| `name` | string | free text |
| `address` | int | Modbus slave address |
| `meter_config` | int | must be an existing `electric_meter_config.id`, else FK violation → 500 |

Behaviour: **idempotent upsert-by-lookup** on `(name, address)`. If a row with that exact pair
exists, it is returned unchanged with status **201** (not 200). Otherwise a row is inserted with
`active = 1` and `created_on = datetime.now(timezone.utc).isoformat()`.

Response `201` — `MeterResponse`:

```json
{ "id": 11, "name": "Meter A", "address": 1, "meter_config": 1, "active": true, "created_on": "2026-10-06T07:12:34.567890+00:00" }
```

`active` is a real JSON boolean here (coerced from the stored 0/1). `created_on` is nullable in the
response model.

### 8.12 `PUT /meters/{meter_id}` — partial update

`meter_id`: `int` path parameter, the **PK**. Request (`UpdateMeterRequest`) — every field optional;
only present, non-null fields are written:

```json
{ "name": "Meter A2", "address": 2, "meter_config": 1, "active": false }
```

Responses: `200` with `MeterResponse` · `404` `{"detail": "Meter with id 99 not found"}` ·
`400` `{"detail": "No fields to update"}` when the body has no non-null field · `500` otherwise.

Correct parameterised dynamic SQL; `active` is stored as `int(bool)`.

### 8.13 `GET /settings/` — read configuration

**The trailing slash is required** — `GET /settings` returns a `307` redirect.

Response `200` — the in-memory `Settings` dataclass:

```json
{
  "Modbuss_RTU": { "port": "COM2", "baudrate": 9600, "parity": "N", "stopbits": 1, "bytesize": 8, "timeout": 1 },
  "Remote_Database": { "url": "Hello!", "user": "", "password": "", "db_name": "" },
  "Local_Database": { "db_name": "meter.db" }
}
```

**This serves a cached snapshot**, built once at import time in `router/constants.py:6`. It reflects
`settings.json` as it was when the process started, not necessarily what is on disk now.

**Security note:** `Remote_Database.password` is returned in cleartext to any anonymous caller.
Whatever credentials the server hands the edge for upstream sync will be readable by anyone who can
reach port 8000.

### 8.14 `POST /settings/` — write configuration

Trailing slash required. Body is the complete `Settings` object — **all three sections are
mandatory**; this is a full replace, not a patch. Shape exactly as §8.13.

Behaviour: writes `app/settings.json` (resolved as `dirname(settings_service.py)/../settings.json`,
so it is CWD-independent — unlike the *load* path) with `indent=4, sort_keys=True`, and echoes the
posted object back with `200`.

**[AS-IS DEFECT]** It does **not** refresh the cached `SETTINGS` or rebuild `SETTINGS_CLIENT`, and
the hardcoded `COM3` clients in `reading/reading_logic.py` ignore the file in any case. **A
configuration change has no effect until the process restarts**, and the serial port used by
`/voltage*` never changes at all. `GET /settings/` will keep returning the old values until restart,
so a server cannot use read-after-write to confirm a config change.

### 8.15 `GET /time/sheet` — list polling schedules

No pagination, no filtering. Response `200` — a JSON array of serialised `TimeSheetEntity`, ordered
`ORDER BY ts.created_on DESC`, `INNER JOIN` to `electric_meter`:

```json
[
  {
    "id": 10,
    "name": "Default 60-min polling",
    "interval": 60,
    "is_active": true,
    "meter": { "id": 10, "name": "Priem UHT", "address": 10, "created_on": "2026-09-29T04:34:17" },
    "created_at": "2026-09-29T04:34:17"
  }
]
```

Contract details:

- `interval` is **minutes**.
- `is_active` is a real JSON boolean.
- The outer timestamp key is **`created_at`** (the dataclass field) while the column is
  `created_on` — and the nested meter object uses **`created_on`**. Both appear in one payload.
- The nested `meter` object is the narrow `ElectricMeterEntity`: **no `meter_config`, no `active`**.
- Timestamps are ISO-8601 **without** a UTC offset for seeded rows (they were written by SQLite's
  `datetime('now')` and re-emitted by `datetime.fromisoformat`), so they are naive. Rows created by
  other writers may be offset-aware. See §14.1.

### 8.16 `POST /time/sheet/scheduler/start` — re-arm the scheduler

**Takes no body.** Calls `schedule.clear()`, re-reads all time sheets and read configs, re-registers
jobs, performs **one immediate control read per active meter**, and ensures the daemon thread is
running.

Response `200`: `{ "status": "scheduler started" }`

Side effects a caller must understand:

- The scheduler is **already started** by the app's lifespan hook, so this is a re-arm, not a start.
- Every call writes **one extra reading row per active meter** (the control run), outside the normal
  cadence.
- Interval phases reset: a 60-minute job re-armed at `:37` next fires at `:37`, not on the hour.
- It is the **only** way to pick up time-sheet changes without a restart.

**[AS-IS DEFECT]** `bruno/README.md` and the `Start scheduler.bru` docs claim this registers two
hardcoded schedules (meters 1 and 2, at 1 and 5 minutes) and returns `null`. That describes an
older implementation; the current code is as documented above. The Bruno docs are stale.

### 8.17 FastAPI built-ins

`GET /openapi.json` · `GET /docs` (Swagger UI, HTML) · `GET /redoc` (ReDoc, HTML).

**[AS-IS DEFECT]** Three handler names are each defined more than once in
`electric_energy_routs.py` — `get_voltage` (lines 16, 31), `get_all_electric_meters_with_data`
(46, 56), `get_energy_new` (66, 117, 178). Routing works (each decorator runs at definition time),
but FastAPI derives `operationId` from the function name, so **the generated OpenAPI document has
colliding operation IDs**. Any code generator run against `/openapi.json` will silently drop
operations. Do not generate the server's edge client from this schema until the duplicates are
renamed.

---

## 9. GraphQL contract

`app/router/settings_graph_routs.py` — Strawberry GraphQL mounted at **`/settings/graphql`**
(router prefix `/settings` + `/graphql`). `POST` for queries, `GET` serves the GraphiQL IDE.

The complete schema, verbatim:

```graphql
type User { name: String!  age: Int! }
type Query { user: User! }
```

`user` returns a hardcoded `User(name: "Patrick", age: 100)`. There are **no mutations, no
subscriptions, and no connection to settings or meter data** despite the path. It is a placeholder.

**Server verdict:** no usable GraphQL contract exists. Treat the path as reserved.

---

## 10. Configuration contract (`settings.json`)

### 10.1 Canonical file

`app/settings.json` — current contents (note the keys land alphabetically because
`Settings.to_json()` uses `sort_keys=True`):

```json
{
    "Local_Database": { "db_name": "meter.db" },
    "Modbuss_RTU": {
        "baudrate": 9600,
        "bytesize": 8,
        "parity": "N",
        "port": "COM2",
        "stopbits": 1,
        "timeout": 1
    },
    "Remote_Database": { "db_name": "", "password": "", "url": "Hello!", "user": "" }
}
```

### 10.2 Field reference

| Section | Key | Type | Consumed by | Effect |
|---|---|---|---|---|
| `Modbuss_RTU` | `port` | string | `SETTINGS_CLIENT` only | `COM2`/`COM3` on Windows, `/dev/ttyUSB0` on Linux. **Ignored by `/voltage*` and `/electric/all`'s live reads**, which hardcode `COM3`. |
| | `baudrate` | int | same | 9600 |
| | `parity` | string | same | `"N"`, `"E"`, `"O"` |
| | `stopbits` | int | same | 1 |
| | `bytesize` | int | same | 8 |
| | `timeout` | int | same | seconds |
| `Remote_Database` | `url`, `user`, `password`, `db_name` | string | **nothing** | Parsed, cached, served over `GET /settings/`. **Dead configuration — the server-connection hook.** |
| `Local_Database` | `db_name` | string | **nothing** | The actual DB path is hardcoded (`repository/meter.db`), not derived from this. |

### 10.3 Loading rules and hazards

- `load_settings(path="settings.json")` — **relative to the current working directory**, so the app
  must run from `app/`. All three sections and every key are mandatory: a missing key raises
  `KeyError` at import time and the process fails to start.
- There is a **second, incompatible `settings.json` at the repository root** containing only
  `{"Modbuss_RTU": {...}}` (and `port: "COM3"`). If the app is ever started from the repo root, the
  load raises `KeyError: 'Remote_Database'`. It is a stale artifact — do not treat it as a template.
- Writes go to `app/settings.json` (CWD-independent); reads come from `./settings.json`
  (CWD-dependent). The two paths only agree when the CWD is `app/`.

---

## 11. Scheduler / polling contract

This is the pipeline that produces every persisted reading — the data the server will ultimately
collect.

Implementation: `app/service/timesheet/time_sheet_service.py`, function
`activate_time_sheets_service_con(conn)` plus `time_sheet_read_job(...)`, using the third-party
`schedule` library and one daemon thread.

### 11.1 Arming

1. `schedule.clear()` — drops all previously registered jobs.
2. `get_time_sheets_with_meters(conn)` → all time sheets (joined to meters), `created_on DESC`.
3. `get_all_electric_meters_read_config(conn)` → one `ReadElectricMeter` per meter, filtered
   `WHERE address_to = 440`, `ORDER BY em.address ASC`. With the current data this yields exactly
   ten descriptors with `device_id = em.address`, `length = 4` (from `register_length`),
   `label = em.name`, `offset_address = [401, 440]`.
4. The connection is **closed immediately** (`conn.close()`), while the caller in
   `time_sheets_routs.py` still holds the same object — it is never reused, so this is benign today.
5. Time sheets are indexed into `{meter.address: time_sheet}`. A code comment explains that an
   earlier `zip()` pairing mismatched meters against other meters' intervals because the two queries
   order differently; the dict fixed that. **Only one time sheet per address survives the dict.**
6. For each read config:
   - no matching time sheet → skipped with a log line
   - `is_active` false → skipped
   - otherwise a **fresh `ReadElectricMeter`** is built: `device_id` from the read config,
     `length=4`, `label` the meter name, **`offset_address=[801, 840]` hardcoded** (the DB's
     `[401, 440]` is discarded), `action=decode_double_len4`, and a **new `ModbusSerialClient`** per
     meter from `settings.json`.
   - `schedule.every(interval).minutes.do(time_sheet_read_job, readMeterRequest=…, meter_id=time_sheet.meter.id)`
     — registration happens **before** the control read, deliberately: an earlier version did the
     control read first, so a meter offline at startup raised and never got registered at all.
   - then one **immediate control read** runs synchronously.

### 11.2 The job (`time_sheet_read_job`)

Documented as "never raises" — every stage reports and returns, because an escaping exception would
stop all future polls.

1. `read_energy(readMeterRequest)` → ten doubles (layout A, §6.3). On failure: print and return —
   **nothing is persisted, nothing is logged to `error_log`, and no gap marker is recorded.**
2. Open a fresh connection to `BASE_DIR/repository/meter.db` (absolute, CWD-independent — unlike the
   HTTP read paths). Resolve `meter_id`: use the passed PK, else
   `get_electric_meter_id_by_address(device_id)`. If unresolvable, print and return without
   inserting (correctly avoiding an FK violation).
3. `insert_energy_reading(...)` with `created_on = datetime.now(timezone.utc)` → ISO-8601 with
   `+00:00`. **This single INSERT is the authoritative write path for all time-series data.**
4. Best-effort Excel append (§12). Deliberately after the DB commit, so a spreadsheet locked by
   Excel cannot cost a reading. Failure is a warning only.

### 11.3 Runner thread

`start_scheduler_thread()` starts a daemon thread running `schedule.run_pending()` with a 1 s sleep,
wrapped in a `try/except` that prints and continues. A comment records why: one serial timeout or a
locked `output.xlsx` used to propagate out of `run_pending()`, kill the thread, and leave
`_scheduler_running = True` so no replacement could start — all polling stopped until restart.
Restart safety now also checks `is_alive()`. `stop_scheduler_thread()` just clears the flag; the
thread exits within ~1 s.

### 11.4 Timing characteristics the server must expect

- Cadence is **interval minutes since arming**, not wall-clock aligned. A 60-minute job armed at
  `:37` fires at `:37`. There is no `at(":00")` alignment on the live path.
- **Jitter and drift are unbounded**: `schedule` fires jobs sequentially on one thread, and each job
  blocks for the full serial read. Ten meters with a 1 s timeout each can push later jobs seconds to
  minutes late.
- Every arming adds one off-cadence control reading per meter (startup, and every
  `POST /time/sheet/scheduler/start`).
- **Failed reads leave silent gaps.** Nothing records that a poll was attempted and failed. A server
  cannot distinguish "meter offline" from "edge offline" from "meter deleted" by looking at the data.
- The clock is the edge host's clock. There is no NTP guarantee documented anywhere, and timestamps
  are generated host-side, so **out-of-order and future-dated readings are possible** after a clock
  correction.

### 11.5 Dead scheduler variants — do not resurrect

The same module contains `activate_time_sheets`, `activate_time_sheets_service`,
`activate_time_sheets_service_v3`, `activate_time_sheets_service_v4`, plus a module-level
`DB_LOCATION = "repository/meter.db"` used only by those. `_v3` and `_v4` both end in a bare
`except: pass`; `_v3` has an unreachable `while True` loop and indexes a `datetime` as if it were a
dict. `custom_time_sheet_service.py` holds two more variants with busy-wait loops.
`service/services.py` is worse: it has a **module-level call** `read_meter_date(1)` at line 60 that
executes on import, against a relative `"../repository/meter.db"` path. Only
`activate_time_sheets_service_con` is wired up.

---

## 12. Excel export contract

`app/repository/excel_repository.py`, called from `time_sheet_read_job` with
`filename="output_markeli.xlsx"`.

| Aspect | Contract |
|---|---|
| Destination 1 | `~/Documents/output_markeli.xlsx` (created if absent) |
| Destination 2 | `C:\Users\ПРИЕМНО\OneDrive\energy-markeli\output_markeli.xlsx` — **a hardcoded absolute Windows path with a Cyrillic username**. On Linux or any other machine this silently takes the error branch. |
| Worksheet per | Calendar day, named `YYYY-MM-DD` from `datetime.now()` (**local** time) |
| Header row | `Date`, `Time`, `Meter Name`, `Active Energy import`, `Active Energy export`, `Reactive Energy import`, `Reactive Energy export`, `Apparent Energy` |
| Row | `str(datetime.now())`, `str(datetime.now().time())`, `label` (the meter name), then five values |
| Decimal separator | **comma** — `str(value).replace(".", ",")`, for a BG-locale Excel |
| Columns dropped | Tariff-2 totalisers and both apparent-tariff variants: only `…Tariff1` of active import/export, reactive import/export, and apparent are written. **Half the measured data never reaches the spreadsheet.** |

This file is effectively the current manual export channel to the outside world (via OneDrive). It
is lossy, locale-bound, machine-bound, and has no identifiers (no meter id, no address — only the
name string). **It is not a viable server ingestion source.**

---

## 13. Error and status-code contract

| Code | When | Body |
|---|---|---|
| `200` | success | endpoint-specific |
| `201` | `POST /meters` — both on insert **and** on returning an existing row | `MeterResponse` |
| `307` | `GET /settings` without the trailing slash | redirect to `/settings/` |
| `400` | `PUT /meters/{id}` with no updatable field | `{"detail": "No fields to update"}` |
| `404` | `PUT /meters/{id}` for an unknown id; any unmatched path | `{"detail": "Meter with id N not found"}` / `{"detail": "Not Found"}` |
| `422` | FastAPI request validation failure | standard FastAPI `{"detail": [{"loc": [...], "msg": "...", "type": "..."}]}` |
| `500` | **everything else** | `{"detail": "<str(exception)>"}` |

Every handler in `electric_energy_routs.py` and `time_sheets_routs.py` wraps its body in
`try/except Exception as e: raise HTTPException(500, str(e))`. Consequences for a client:

- **There is no machine-readable error taxonomy.** A Modbus timeout, a missing SQLite table, a
  `RecursionError`, and an `IndexError` are all `500` with a prose `detail`. Distinguishing them
  requires string-matching Python exception messages.
- Expect these `detail` strings in normal operation:
  - `Failed to connect to Modbus device` — no hardware / wrong port
  - `Modbus Error: [Input/Output] No response received after 3 retries, continue with next request`
  - `no such table: electric_meter` — wrong working directory (§2)
  - `maximum recursion depth exceeded` — `POST /meter`
  - `list index out of range` — `GET /all/totenergy`
- There are **no timeouts on the server side** of a live read beyond the 1 s serial timeout per
  attempt, multiplied by the number of meters. A client calling `/electric/all` or `/all/totenergy`
  against a dead bus should set its own generous timeout (≥ 60 s) or avoid those endpoints.
- No `Retry-After`, no rate limiting, no idempotency keys (except `POST /meters`' natural
  idempotency on `(name, address)`).

---

## 14. Cross-cutting contract hazards

Each of these will cause a subtle server-side bug if not handled explicitly.

### 14.1 Timestamps — five formats in four columns

| Column / field | Written by | Format | Zone |
|---|---|---|---|
| `electric_meter.created_on` (seeded) | SQLite `datetime('now')` | `2026-09-29 04:34:15` | UTC, **no marker** |
| `electric_meter.created_on` (`POST /meters`) | `datetime.now(timezone.utc).isoformat()` | `2026-10-06T07:12:34.567890+00:00` | UTC, explicit |
| `electric_meter_energy_readings.created_on` | `datetime.now(timezone.utc).isoformat()` | `…+00:00` | UTC, explicit |
| `time_sheet.created_on` (seeded) | `datetime('now')` | `2026-09-29 04:34:15` | UTC, no marker |
| `error_log.created_on` | `datetime.now(timezone.utc).isoformat()` | `…+00:00` | UTC, explicit |
| `electric_meter_read_config.created_on` | seed literal | **`22.01.2026`** (dd.mm.yyyy) | none |
| `/electric/all` → `lastUpdated`, `peakTime`, `alerts[].date` | `str(datetime.now())` | `2026-10-06 07:12:34.567890` | **LOCAL, no marker** |
| `/time/sheet` → `created_at`, `meter.created_on` | `datetime.fromisoformat(col)` re-encoded | ISO, offset only if the column had one | mixed |

Consequences:

- **The same column holds both space-separated and `T`-separated values** depending on which writer
  created the row. `ORDER BY created_on DESC` is a *lexicographic string sort* in SQLite — and
  `'2026-09-29 04:34'` sorts **before** `'2026-09-29T04:34'` because space < `T`. Mixed-format rows
  therefore order incorrectly, which silently corrupts "latest reading" lookups
  (`found_energy[0]` in `/electric/all`, and the dashboard's `LIMIT 100`).
- `/electric/all`'s timestamps are **local** while everything persisted is **UTC**. The one
  timestamp a server would naively read off that endpoint is in a different zone from the data.
- A server must parse defensively (accept space or `T`, offset present or absent), **assume UTC when
  no offset is present** for DB-sourced values, and never assume lexicographic ordering equals
  chronological ordering.

### 14.2 Units — four conflicting labels for the same stored number

| Surface | Active energy | Reactive | Apparent |
|---|---|---|---|
| Database (`REAL`) | *(unitless raw double)* | — | — |
| `/totenergy`, `/anything/len4` response labels | `"Wh"` | `"varh"` | `"VAh"` |
| `/electric/all` → `energyData.*Unit` | `"kWh"` | `"varh"` | `"VAh"` |
| Internal `ReadElectricMeter.unit` for the same read | `"KWh"` / `"kw"` | — | — |
| Excel header | *(no unit)* | — | — |

`/totenergy` says `Wh`, `/electric/all` says `kWh`, and the internal descriptor for the scheduler's
read says `"kw"` — **for the same registers**. No code performs any scaling anywhere.

**Server rule:** the unit of a stored totaliser is a property of the **register map** (PAC3120
registers 801+), not of any JSON field. Pin the unit once, in server configuration, per meter
model/register-map version. Discard every `*_unit` / `*Unit` string the API emits.

### 14.3 Number-or-string unions

`/electric/all` emits `lastReading` and all ten `energyData` values as the **string `"0"`** when a
meter has no stored reading, and as a **JSON number** otherwise. `peakLoad` is always the string
`"0"`. A strongly-typed server client must model these as unions or coerce.

### 14.4 Three naming conventions for one set of fields

| Convention | Where | Example |
|---|---|---|
| `PascalCase` | DB columns, entities, DTOs | `TotalActiveEnergyImportTariff1` |
| `camelCase` | `/electric/all` → `energyData` | `totalActiveEnergyImportTariff1` |
| `Snake_Case_Capitalised` | `/totenergy`, `/anything/len4` | `Total_active_energy_import_tariff_1` |

Plus `created_on` vs `created_at` for the same concept within a single `/time/sheet` payload. A
server-side mapping table is mandatory; do not rely on a case-insensitive auto-mapper.

### 14.5 Concurrency and consistency

- SQLite with the default journal mode; **no WAL configured**. Concurrent readers during the
  scheduler's write can hit `database is locked`.
- Router read paths call `sqlite3.connect(DB_LOCATION)` per request and **never close** the
  connection, and do not use a context manager.
- `PRAGMA foreign_keys = ON` is per-connection and is applied inconsistently, so FK violations are
  enforced on some paths and not others.
- The shared `SETTINGS_CLIENT` serial client is used from both HTTP handlers and the scheduler
  thread with no lock — **interleaved Modbus frames on the RS-485 bus are possible** when a live-read
  endpoint is called while a poll is in flight.
- The desktop dashboard writes meters and time sheets straight to the same file, so the HTTP API is
  not the only writer.

### 14.6 Capability gaps

- **No history endpoint.** `electric_meter_energy_readings` is reachable over HTTP only as the single
  latest row per meter, embedded in `/electric/all`. There is no `since`/`until`, no pagination, no
  bulk export.
- **No delete** for meters or time sheets over HTTP.
- **No time-sheet create/update** over HTTP (the dashboard does it in SQL directly).
- **No read-config / register-map endpoint.**
- **No `error_log` endpoint.**
- **No health endpoint beyond `GET /`** — nothing reports scheduler liveness, last successful poll,
  serial-port state, or meter reachability. `edge-monitoring` derives this by reading SQLite
  directly (`StatusCommand` counts rows; `LastReadCommand` finds the newest reading per meter).

---

## 15. Requirements this imposes on the server

Derived directly from the sections above. These are the constraints any server implementation has
to satisfy to interoperate with the edge device as it exists.

### 15.1 Identity

1. **Supply a device/site identity out of band.** No payload or table carries one. Provision it as
   server-issued configuration (`Remote_Database` is the existing, unused slot) and key everything
   server-side on `(device_id, electric_meter.id)`.
2. **Never use `electric_meter.address` as an identity.** It is a bus address, it is 1..10 on every
   site, and it is editable via `PUT /meters/{id}`.
3. **Never assume `id == address`**, even though it holds in the current deployment.
4. Treat `electric_meter.name` as a mutable, non-unique, non-ASCII display label.

### 15.2 Data model alignment

5. Mirror `electric_meter_energy_readings` **column-for-column, including PascalCase names**, and
   store the values as `double precision` with **no scaling**. Pin units in server configuration,
   keyed by meter model / register-map version — not from any API field.
6. Model readings as **cumulative monotonic totalisers**. Compute consumption as a difference over a
   window, and detect resets (a value decreasing) explicitly — a meter swap or reset will produce
   one.
7. Expect, and de-duplicate, **off-cadence control readings** at every edge startup and every
   scheduler re-arm. Deduplicate on `(device, meter_id, created_on)` and tolerate rows seconds apart.
8. Expect **gaps with no explanation**. Failed polls leave no trace. Infer liveness from the arrival
   of readings, not from their absence.
9. Parse timestamps defensively per §14.1: accept both `YYYY-MM-DD HH:MM:SS[.ffffff]` and ISO `T`
   form, with or without an offset; **assume UTC when the offset is missing**; normalise to UTC on
   ingest; and never rely on string ordering for chronology. Tolerate out-of-order and future-dated
   rows (edge clock).
10. Store the edge's `electric_meter_read_config` (register map) alongside the data if the server
    needs to interpret registers — but know that the stored map (`401..440`) **does not match what
    the code reads (`801..840`)**, and the code wins.

### 15.3 Transport and collection

11. **The edge cannot push.** It has no outbound client at all. The server must either (a) poll the
    edge's HTTP API, (b) have an agent added to the edge, or (c) read the SQLite file over the
    existing WireGuard tunnel. Option (b) is the only one that gives at-least-once delivery with
    acknowledgement.
12. **Do not ingest `GET /electric/all` as the data feed.** It carries only the latest reading, mixes
    in hardcoded placeholders, unions numbers with strings, timestamps in local time with the *call*
    time rather than the *reading* time, and costs one serial timeout per meter per call. Use it for
    UI only.
13. For collection over HTTP, the edge needs a **new endpoint** shaped roughly as
    `GET /readings?since=<iso>&limit=<n>` returning raw `electric_meter_energy_readings` rows with
    their PKs and `created_on`, ordered by `id` (stable, unlike `created_on`). Design it around
    monotonic `id` cursors, not timestamps.
14. Assume **no authentication and no TLS** today. Anything the server exposes to the edge, or the
    edge to the server, needs a transport that supplies both — the WireGuard tunnel is the only
    existing candidate. Note that `POST /anything/len2|len4` is an unauthenticated raw Modbus
    passthrough: do not expose port 8000 beyond the tunnel.
15. **Do not store server credentials in `settings.json`.** `GET /settings/` returns
    `Remote_Database.password` in cleartext to any anonymous caller on the LAN.
16. Set client timeouts generously (≥ 60 s) for any endpoint that touches the serial bus, or avoid
    those endpoints entirely.

### 15.4 Control plane

17. **Configuration changes do not take effect until the edge restarts**, and `GET /settings/` keeps
    serving the stale cached values, so read-after-write cannot confirm a change. A server-driven
    config flow needs either a restart mechanism or an edge-side cache invalidation fix.
18. The only way to apply a time-sheet change without a restart is `POST /time/sheet/scheduler/start`
    — which also writes an extra reading per meter and resets interval phases. Budget for both.
19. Time-sheet CRUD does not exist over HTTP. Either add it, or accept that schedules are managed
    on-device.
20. `POST /meters` is safely retryable (idempotent on `(name, address)`), but returns **201 for an
    existing row** — do not treat 201 as proof of creation.

### 15.5 Endpoints to avoid entirely

21. `POST /meter` (always 500) · `GET /electric/{meter_id}}` (broken path, returns `null`) ·
    `GET /all/totenergy` (always 500) · `/settings/graphql` (placeholder schema) · the `Endpoints/`
    collection (abandoned).
22. **Do not generate the server's edge client from `/openapi.json`** until the duplicate handler
    names in `electric_energy_routs.py` are renamed — colliding `operationId`s make generators drop
    operations silently.

### 15.6 Things to fix on the edge first, if the server is to be trusted

In rough priority order, with the server-visible consequence of leaving each one alone:

| Fix | Consequence if skipped |
|---|---|
| `electric_service.py:96` — `meter.address` → `meter.id` | `/electric/all` misattributes readings as soon as `id ≠ address` |
| Duplicate handler names | generated clients silently lose endpoints |
| Add a `GET /readings?since=…` history endpoint | no ingestion path for the time series |
| Add a device/site identifier to config and to payloads | multi-site data cannot be disambiguated |
| Add authentication (even a static token) + keep port 8000 tunnel-only | unauthenticated raw Modbus read access |
| Normalise `created_on` to one format, or switch ordering to `id` | "latest reading" lookups order wrongly on mixed-format rows |
| Log failed polls to `error_log` | gaps are indistinguishable from downtime |
| Refresh `SETTINGS`/`SETTINGS_CLIENT` on `POST /settings/` | remote configuration is write-only until restart |

---

## Appendix A — canonical JSON payloads

Collected for copy-paste into server-side fixtures and tests. Numeric values are illustrative; key
names, casing and types are exact.

### A.1 `GET /` 200

```json
{ "message": "Hello Bigger Applications!" }
```

### A.2 `GET /voltage` 200

```json
{ "name": "Voltage", "l1": 231.4, "l2": 229.8, "l3": 230.1, "unit": "V" }
```

### A.3 `GET /electric/all` 200 — one element, no stored reading, no hardware

```json
[
  {
    "id": "TBA 8 Sentron",
    "name": "TBA 8",
    "location": " Locations",
    "status": "active",
    "groupId": "none",
    "lastReading": "0",
    "lastUpdated": "2026-10-06 07:12:34.567890",
    "dailyAverage": 0,
    "peakLoad": "0",
    "peakTime": "2026-10-06 07:12:34.567890",
    "monthlyCost": 0,
    "rate": 0,
    "type": "Modbus Meter",
    "voltage": "690 V AC (L-L) / 400 V AC (L-N)",
    "phase": 3,
    "installDate": " ",
    "manufacturer": "Simens",
    "alerts": [ { "message": " ", "date": "2026-10-06 07:12:34.567890", "severity": "low" } ],
    "voltageData": { "name": "", "l1": 0, "l2": 0, "l3": 0, "unit": "" },
    "amperageData": { "name": "", "l1": 0, "l2": 0, "l3": 0, "unit": "" },
    "energyData": {
      "totalActiveEnergyImportTariff1": "0",
      "totalActiveEnergyImportTariff1Unit": "kWh",
      "totalActiveEnergyImportTariff2": "0",
      "totalActiveEnergyImportTariff2Unit": "kWh",
      "totalActiveEnergyExportTariff1": "0",
      "totalActiveEnergyExportTariff1Unit": "kWh",
      "totalActiveEnergyExportTariff2": "0",
      "totalActiveEnergyExportTariff2Unit": "kWh",
      "totalReactiveEnergyImportTariff1": "0",
      "totalReactiveEnergyImportTariff1Unit": "varh",
      "totalReactiveEnergyImportTariff2": "0",
      "totalReactiveEnergyImportTariff2Unit": "varh",
      "totalReactiveEnergyExportTariff1": "0",
      "totalReactiveEnergyExportTariff1Unit": "varh",
      "totalReactiveEnergyExportTariff2": "0",
      "totalReactiveEnergyExportTariff2Unit": "varh",
      "totalApparentEnergyTariff1": "0",
      "totalApparentEnergyTariff1Unit": "VAh",
      "totalApparentEnergyTariff2": "0",
      "totalApparentEnergyTariff2Unit": "VAh"
    }
  }
]
```

With a stored reading and working hardware, `lastReading` and every `energyData` value become
numbers, and `voltageData` / `amperageData` carry
`{"name": "Voltage"|"Amperage", "l1": …, "unit": "V"|"A"}`.

### A.4 `GET /totenergy` 200

```json
{
  "Total_active_energy_import_tariff_1": 123456.789,
  "Total_active_energy_import_tariff_2": 0.0,
  "Total_active_energy_export_tariff_1": 0.0,
  "Total_active_energy_export_tariff_2": 0.0,
  "Total_reactive_energy_import_tariff_1": 4567.89,
  "Total_active_energy_import_tariff_1_unit": "Wh",
  "Total_active_energy_import_tariff_2_unit": "Wh",
  "Total_active_energy_export_tariff_1_unit": "Wh",
  "Total_active_energy_export_tariff_2_unit": "Wh",
  "Total_reactive_energy_import_tariff_1_unit": "varh"
}
```

### A.5 `GET /totenergy/import` 200

```json
{ "Total_active_energy_import": 123456.789, "Total_active_energy_import_tariff_1_unit": "Wh" }
```

### A.6 `POST /anything/len4` request / 200 response

```json
{ "device_id": 1, "label": "Tot_energy", "offset_address": [801, 840], "unit": "KWh" }
```

```json
{
  "Total_active_energy_import_tariff_1": 123456.789,
  "Total_active_energy_import_tariff_2": 0.0,
  "Total_active_energy_export_tariff_1": 0.0,
  "Total_active_energy_export_tariff_2": 0.0,
  "Total_reactive_energy_import_tariff_1": 4567.89,
  "Total_reactive_energy_import_tariff_2": 0.0,
  "Total_reactive_energy_export_tariff_1": 0.0,
  "Total_reactive_energy_export_tariff_2": 0.0,
  "Total_apparent_energy_tariff_1": 98765.4,
  "Total_apparent_energy_tariff_2": 0.0,
  "Total_active_energy_import_tariff_1_unit": "Wh",
  "Total_active_energy_import_tariff_2_unit": "Wh",
  "Total_active_energy_export_tariff_1_unit": "Wh",
  "Total_active_energy_export_tariff_2_unit": "Wh",
  "Total_reactive_energy_import_tariff_1_unit": "varh",
  "Total_reactive_energy_import_tariff_2_unit": "varh",
  "Total_reactive_energy_export_tariff_1_unit": "varh",
  "Total_reactive_energy_export_tariff_2_unit": "varh",
  "Total_apparent_energy_tariff_1_unit": "VAh",
  "Total_apparent_energy_tariff_2_unit": "VAh"
}
```

### A.7 `POST /anything/len2` request / 200 response

```json
{ "device_id": 1, "label": "Voltage_L1", "offset_address": [1, 6], "unit": "V" }
```

```json
{ "tot_energy": [231.4, 229.8, 230.1], "unit": "Wh" }
```

### A.8 `POST /meters` request / 201 response

```json
{ "name": "Meter A", "address": 1, "meter_config": 1 }
```

```json
{ "id": 11, "name": "Meter A", "address": 1, "meter_config": 1, "active": true, "created_on": "2026-10-06T07:12:34.567890+00:00" }
```

### A.9 `PUT /meters/{meter_id}` request / 200 response

```json
{ "name": "Meter A2", "active": false }
```

```json
{ "id": 11, "name": "Meter A2", "address": 1, "meter_config": 1, "active": false, "created_on": "2026-10-06T07:12:34.567890+00:00" }
```

### A.10 `GET /settings/` 200 · `POST /settings/` request and 200 response

```json
{
  "Modbuss_RTU": { "port": "COM2", "baudrate": 9600, "parity": "N", "stopbits": 1, "bytesize": 8, "timeout": 1 },
  "Remote_Database": { "url": "Hello!", "user": "", "password": "", "db_name": "" },
  "Local_Database": { "db_name": "meter.db" }
}
```

### A.11 `GET /time/sheet` 200

```json
[
  {
    "id": 10,
    "name": "Default 60-min polling",
    "interval": 60,
    "is_active": true,
    "meter": { "id": 10, "name": "Priem UHT", "address": 10, "created_on": "2026-09-29T04:34:17" },
    "created_at": "2026-09-29T04:34:17"
  }
]
```

### A.12 `POST /time/sheet/scheduler/start` 200

```json
{ "status": "scheduler started" }
```

### A.13 Error bodies

```json
{ "detail": "Failed to connect to Modbus device" }
```

```json
{ "detail": "Meter with id 99 not found" }
```

```json
{ "detail": [ { "loc": ["body", "address"], "msg": "field required", "type": "value_error.missing" } ] }
```

### A.14 A persisted reading row (the shape the server should mirror)

```json
{
  "id": 1,
  "meter_id": 1,
  "TotalActiveEnergyImportTariff1": 123456.789,
  "TotalActiveEnergyImportTariff2": 0.0,
  "TotalActiveEnergyExportTariff1": 0.0,
  "TotalActiveEnergyExportTariff2": 0.0,
  "TotalReactiveEnergyImportTariff1": 4567.89,
  "TotalReactiveEnergyImportTariff2": 0.0,
  "TotalReactiveEnergyExportTariff1": 0.0,
  "TotalReactiveEnergyExportTariff2": 0.0,
  "TotalApparentEnergyTariff1": 98765.4,
  "TotalApparentEnergyTariff2": 0.0,
  "created_on": "2026-10-06T07:12:34.567890+00:00"
}
```

No endpoint returns this shape today. It is what a history endpoint should return, and what the
server's own table should look like plus a device/site key.

---

## Appendix B — full DDL

Verbatim from `app/resources/scripts/create.sql`. All statements are `IF NOT EXISTS`, so startup is
idempotent. `app/resources/scripts/drop.sql` drops them in FK-safe order but **omits `error_log`**.

```sql
CREATE TABLE iF NOT EXISTS electric_meter_read_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    address_from TEXT NOT NULL,
    address_to TEXT NOT NULL,
    version INTEGER NOT NULL,
    register_length INTEGER NOT NULL,
    created_on TEXT
);

CREATE TABLE IF NOT EXISTS electric_meter_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    make TEXT NOT NULL,
    model TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS mm_meter_read_config_meter_read_config(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    electric_meter_config INTEGER NOT NULL,
    electric_meter_read_config INTEGER NOT NULL,
    FOREIGN KEY (electric_meter_config) REFERENCES electric_meter_config(id) ON DELETE CASCADE,
    FOREIGN KEY (electric_meter_read_config) REFERENCES electric_meter_read_config(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS electric_meter (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    address INTEGER NOT NULL,
    meter_config INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_on TEXT,
    FOREIGN KEY (meter_config) REFERENCES electric_meter_config(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS electric_meter_energy_readings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    meter_id INTEGER NOT NULL,
    TotalActiveEnergyImportTariff1 REAL NOT NULL,
    TotalActiveEnergyImportTariff2 REAL NOT NULL,
    TotalActiveEnergyExportTariff1 REAL NOT NULL,
    TotalActiveEnergyExportTariff2 REAL NOT NULL,
    TotalReactiveEnergyImportTariff1 REAL NOT NULL,
    TotalReactiveEnergyImportTariff2 REAL NOT NULL,
    TotalReactiveEnergyExportTariff1 REAL NOT NULL,
    TotalReactiveEnergyExportTariff2 REAL NOT NULL,
    TotalApparentEnergyTariff1 REAL NOT NULL,
    TotalApparentEnergyTariff2 REAL NOT NULL,
    created_on TEXT NOT NULL,
    FOREIGN KEY (meter_id) REFERENCES electric_meter(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS time_sheet (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    meter_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 1,
    interval INTEGER NOT NULL,
    created_on TEXT NOT NULL,
    FOREIGN KEY (meter_id) REFERENCES electric_meter(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS meter_group(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    meter_id INTEGER NOT NULL,
    description TEXT NOT NULL,
    created_on TEXT
);

CREATE TABLE IF NOT EXISTS mm_meter_group_electric_meter(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    meter_id INTEGER NOT NULL,
    group_id INTEGER NOT NULL,
    FOREIGN KEY (meter_id) REFERENCES electric_meter(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES meter_group(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS error_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    error_message TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'new',
    created_on TEXT NOT NULL
);
```

---

## Appendix C — file and route inventory

### C.1 Modules that are live vs. dead

| Path | Status |
|---|---|
| `app/main.py` | **live** — the ASGI app |
| `app/router/{constants,electric_energy_routs,meter_routs,settings_routs,settings_graph_routs,time_sheets_routs}.py` | **live** |
| `app/router/graph/__init__.py` | empty package |
| `app/service/{electric_service,settings_service}.py` | **live** |
| `app/service/timesheet/time_sheet_service.py` | **live** — only `activate_time_sheets_service_con`, `time_sheet_read_job`, `start/stop_scheduler_thread` |
| `app/service/timesheet/custom_time_sheet_service.py` | only `start_time_sheet_service` (a `print`) is reachable; the rest is dead |
| `app/service/services.py` | **dead and dangerous** — executes `read_meter_date(1)` at import |
| `app/reading/reading_logic.py` | **live, [PROTECTED]** |
| `app/repository/*.py` | **live**, except `list_tables.py`, `list_meter_data.py`, `meter_simple_select.py` (ad-hoc scripts using bare relative DB paths) |
| `app/{APIEx,UIEx,modbusEX}.py` | standalone scratch scripts, not wired into `main.py`. `APIEx.py` defines its own `db_location` and its own route copies — do not confuse it with `router/`. |
| `app/ui/*` | desktop dashboard — out of scope here; note it writes the same DB directly |
| `app/build.spec` | PyInstaller single-exe build |
| `app/repository/meter1.db` | stale 12-meter DB from the older generic seed |
| `app/meter.db` | 0-byte artifact of running something from the wrong directory |
| `Endpoints/` | abandoned duplicate of `bruno/` — ignore |

### C.2 Existing documentation

| Path | Contents |
|---|---|
| `README.md` | install, run (dashboard / API-only / Debian), Bruno pointer, PyInstaller build |
| `app/run.md` | two-line uvicorn reminder |
| `app/documentation/README.md` | index + "why most endpoints don't work" |
| `app/documentation/architecture.md` | module map, request flow, config/DB conventions, scheduler |
| `app/documentation/endpoint-status.md` | per-endpoint works/broken table |
| `app/documentation/known-bugs.md` | 19 verified bugs with file:line references (#1–#15 from 2026-09-29, #16–#19 added by this document's research pass) |
| `app/documentation/modbus-do-not-modify.md` | the protected-code instruction |
| `app/.claude/skills/project-docs-first/SKILL.md` | read `documentation/` before changing anything |
| `app/.claude/skills/bruno-collection-sync/SKILL.md` | add a `.bru` request for every new route |
| `bruno/README.md` | endpoint table + per-endpoint caveats (partly stale, see §8.1, §8.16) |
| `docs/debian-setup.md` | Linux deployment |
| `docs/vpn-setup.md` | WireGuard Windows ↔ Debian |
| `edge-monitoring/README.md` | Java diagnostics CLI |

### C.3 Defects referenced in this document

Cross-reference to `app/documentation/known-bugs.md`, which carries the full file:line detail:

| # | Defect | known-bugs.md | Here |
|---|---|---|---|
| 1 | `POST /meter` infinite recursion | #1 | §4.1.1, §8.10 |
| 2 | `create_time_sheet` arity mismatch | #2 | §4.1.1 |
| 3 | `/electric/{meter_id}}` path typo | #3 | §8.5 |
| 4 | `get_single_meter_with_data` is a stub | #4 | §8.5 |
| 5 | `insert_electric_meter` omits `meter_config` | #5 | §4.1.1 |
| 6 | `get_all_electric_meter_by_name_and_address` broken four ways | #6 | §4.1.1 |
| 7 | `insert_time_sheet` invalid `:meter.id` binding | #7 | §4.6 |
| 8 | `get_timesheets_…` unbound params + wrong row shape | #8 | — |
| 9 | `address` used as `id` for reading lookup | #9 | §3, §15.6 |
| 10 | `/all/totenergy` ignores the loop variable | #10 | §8.8 |
| 11 | read-config filter hardcodes `address_to = 440` | #11 | §4.3, §11.1 |
| 12 | duplicate handler names → colliding `operationId`s | #12 | §8.17, §15.5 |
| 13 | settings changes need a restart; hardcoded `COM3` clients | #13 | §6.1, §8.14 |
| 14 | DB init/seed errors swallowed | #14 | §2 |
| 15 | dead/legacy code paths | #15 | §11.5, §C.1 |
| 16 | `/all/totenergy` index overflow → always 500 | **#16** (added 2026-10-06) | §8.8 |
| 17 | DB register map (`401..440`) ≠ code (`801..840`) | **#17** (added 2026-10-06) | §4.3, §11.1 |
| 18 | two incompatible energy register layouts (stride 4 vs. `[0,3,7,11,15]`) | **#18** (added 2026-10-06) | §6.3A, §6.3B |
| 19 | mixed timestamp formats break `ORDER BY created_on` | **#19** (added 2026-10-06) | §14.1 |
| 20 | `read_voltage`/`read_amperage` use the address as a register offset | *(not listed — protected code)* | §6.3C |
| 21 | `bruno/README.md` lists a non-existent `/totenergy/{meter_address}` | *(not listed)* | §8.1 |
| 22 | Bruno scheduler docs describe an older implementation | *(not listed)* | §8.16 |
