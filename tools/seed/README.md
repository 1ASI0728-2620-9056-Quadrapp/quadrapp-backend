# Demo seed

`seed.py` creates the demo data of the first deployed version **through the REST API** (never SQL):

| Step | Created | Signed in as |
| --- | --- | --- |
| 1 | Institution `Universidad Demo Quadrapp` | platform administrator |
| 2 | Email domain `campus.quadrapp.test`, verified | platform administrator |
| 3 | Institution administrator `admin@campus.quadrapp.test` (invited and accepted) | platform administrator |
| 4 | Parking lot `Estacionamiento Monterrico` with location and settings | institution administrator |
| 5 | Zones Norte, Central and Sur (report decision D4); Central has two levels; 8 spaces per level (32 in total), the first one of each level accessible | institution administrator |
| 6 | Access `Puerta principal` (bidirectional, flow counting on) | institution administrator |
| 7 | Gateway `demo-gw-01`, one space sensor per space (`demo-ss-n1-01`...) registered **with that gateway** and assigned to its space, and two directional pass sensors (`demo-ps-01`, `demo-ps-02`) on the access | institution administrator |

Parking lot settings, inside the report ranges: saturation 85 % (50-100), flow window 15 min (5-60), minimum detection 5 s for occupied and freed (3-60), accuracy margin 10 % (1-30), stale after 600 s.

The ids and hardware ids are written to `tools/out/seed-output.json` (ignored by git), which the simulator reads.

The API has no `campus` attribute for the parking lot yet (decision D4), so the seed does not send one.

## Requirements

- Python 3.12 or later.
- The backend running (see the root `README.md`, "Run locally").
- In local development, Mailpit: the backend sends the access codes and the invitation by email, and the seed reads them through the Mailpit HTTP API. `docker compose -f tools/docker-compose.dev.yml up -d` starts it (SMTP `127.0.0.1:1025`, HTTP `127.0.0.1:8025`).

```bash
python3 -m venv tools/.venv
tools/.venv/bin/pip install -r tools/seed/requirements.txt
```

## One-time step: the first platform administrator

Only a platform administrator can create an institution and verify its domain, and the API has no way to create the first one. Run this once against the database (the email must match `SEED_PLATFORM_ADMIN_EMAIL`):

```sql
INSERT INTO iam.users (email, role, status, full_name, verified_at)
VALUES ('platform-admin@quadrapp.test', 'PLATFORM_ADMIN', 'ACTIVE', 'Platform Admin', now())
ON CONFLICT (email) DO NOTHING;
```

For example, with the database in a container: `docker exec -i <postgres container> psql -U postgres -d quadrapp -c "<the statement>"`.

If the step is missing, the seed stops with this same statement in its message.

## Run

```bash
tools/.venv/bin/python tools/seed/seed.py
```

Run it again whenever you want: it looks up what exists (by admin email, domain, names, labels and hardware ids) and only creates what is missing. An existing parking lot keeps its settings.

Each run asks for two access codes (platform administrator and institution administrator). The backend allows 5 code requests per email every 15 minutes, so wait if you run it more than twice in a row.

## Configuration

Every option has an environment variable; arguments win over variables.

| Argument | Variable | Default |
| --- | --- | --- |
| `--api-url` | `QUADRAPP_API_URL` | `http://localhost:8080` |
| `--mailpit-url` | `MAILPIT_URL` | `http://127.0.0.1:8025` |
| `--code-source` | `SEED_CODE_SOURCE` | `mailpit`; `prompt` asks you to paste each code and the invitation link (use it when the backend sends real email) |
| `--platform-admin-email` | `SEED_PLATFORM_ADMIN_EMAIL` | `platform-admin@quadrapp.test` |
| `--platform-admin-token` | `SEED_PLATFORM_ADMIN_TOKEN` | none; an access token of a platform administrator skips its sign-in |
| `--institution-name` | `SEED_INSTITUTION_NAME` | `Universidad Demo Quadrapp` |
| `--institution-id` | `SEED_INSTITUTION_ID` | none; needed only when the institution exists but its administrator does not (the API cannot look institutions up by name) |
| `--domain` | `SEED_DOMAIN` | `campus.quadrapp.test` |
| `--admin-email` | `SEED_ADMIN_EMAIL` | `admin@<domain>` |
| `--admin-name` | `SEED_ADMIN_NAME` | `Administrador Demo` |
| `--lot-name` | `SEED_LOT_NAME` | `Estacionamiento Monterrico` |
| `--lot-address`, `--latitude`, `--longitude` | `SEED_LOT_ADDRESS`, `SEED_LATITUDE`, `SEED_LONGITUDE` | UPC Monterrico |
| `--spaces-per-level` | `SEED_SPACES_PER_LEVEL` | `8` |
| `--hardware-prefix` | `SEED_HARDWARE_PREFIX` | `demo`; hardware ids are unique in the whole platform |
| `--output` | `SEED_OUTPUT` | `tools/out/seed-output.json` |

The default domain uses the reserved `.test` top-level domain, so no real mailbox ever receives the codes.

## Output

```json
{
  "tenantId": 1,
  "institution": {"id": 1, "name": "Universidad Demo Quadrapp", "domain": "campus.quadrapp.test"},
  "admin": {"id": 2, "email": "admin@campus.quadrapp.test"},
  "parkingLot": {"id": 1, "occupiedDebounceS": 5, "freedDebounceS": 5, "...": "..."},
  "zones": [{"id": 1, "name": "Norte", "levels": [{"id": 1, "name": "Nivel 1", "spaces": [{"id": 1, "label": "N1-01", "type": "ACCESSIBLE"}]}]}],
  "accesses": [{"id": 1, "name": "Puerta principal", "type": "BIDIRECTIONAL"}],
  "gateways": [{"id": 1, "hardwareId": "demo-gw-01"}],
  "spaceSensors": [{"id": 2, "hardwareId": "demo-ss-n1-01", "spaceId": 1, "spaceLabel": "N1-01", "zone": "Norte", "level": "Nivel 1", "gatewayHardwareId": "demo-gw-01"}],
  "passSensors": [{"id": 34, "hardwareId": "demo-ps-01", "accessId": 1, "gatewayHardwareId": "demo-gw-01"}]
}
```

## Tests

```bash
cd tools/seed && ../.venv/bin/python -m unittest -v
```

The tests need `requests` installed but no backend.
