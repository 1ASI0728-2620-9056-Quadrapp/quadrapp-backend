# Quadrapp Backend

RESTful API of Quadrapp, built as a modular monolith with Spring Boot: one module per bounded context.

## Requirements

- JDK 25
- PostgreSQL 16 or later
- Docker, only to run the integration tests (Testcontainers)

## Run locally

The `dev` profile is required for now: the OTP store and the sensing adapters only have in-memory implementations in that profile.

```bash
export DB_URL=jdbc:postgresql://localhost:5432/quadrapp
export DB_USERNAME=postgres
export DB_PASSWORD=<password>
export JWT_SECRET_BASE64=<base64 key, at least 32 bytes>
export OTP_HASH_PEPPER_BASE64=<base64 value>
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

Optional variables: `MAIL_HOST`, `MAIL_PORT` (Mailpit on `127.0.0.1:1025` by default), `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` and `INVITATION_URL_BASE`.

Sensor readings arrive through an MQTT 5 broker (for example Eclipse Mosquitto). Ingestion is off by default; these optional variables turn it on. The `QUADRAPP_MQTT_*` names bind directly to the properties; the shorter `MQTT_*` names still work.

| Variable | Property | Default | Meaning |
| --- | --- | --- | --- |
| `QUADRAPP_MQTT_ENABLED` (`MQTT_ENABLED`) | `quadrapp.mqtt.enabled` | `false` | Connect to the broker and receive readings |
| `QUADRAPP_MQTT_URL` (`MQTT_URL`) | `quadrapp.mqtt.url` | `tcp://localhost:1883` | Broker URL (`ssl://...` for TLS) |
| `QUADRAPP_MQTT_USERNAME`, `QUADRAPP_MQTT_PASSWORD` (`MQTT_USERNAME`, `MQTT_PASSWORD`) | `quadrapp.mqtt.username`, `quadrapp.mqtt.password` | empty (anonymous) | Backend credential, subscribe only |
| `QUADRAPP_MQTT_CLIENT_ID` (`MQTT_CLIENT_ID`) | `quadrapp.mqtt.client-id` | empty (random id, clean session) | Unique and stable per replica; see below |
| `QUADRAPP_MQTT_SHARED_GROUP` (`MQTT_SHARED_GROUP`) | `quadrapp.mqtt.shared-group` | `sensing` | Shared subscription group: replicas in one group receive each reading once |
| `QUADRAPP_MQTT_RETRY_BACKOFF` (`MQTT_RETRY_BACKOFF`) | `quadrapp.mqtt.retry-backoff` | `200ms` | Pause before the first retry of a failing reading |

Set `QUADRAPP_MQTT_CLIENT_ID` in production, with a different, stable value per replica (for example `quadrapp-backend-1`, `quadrapp-backend-2`). The backend then keeps a persistent session (1 hour), so readings published while a replica restarts wait for it instead of being lost. There is no fixed default because two replicas with the same id disconnect each other, and an id that changes on every start leaves sessions behind that keep receiving their share of readings until they expire. With no id the backend uses a random one and a clean session: safe for any number of replicas, but a reading in flight during a disconnection can be lost.

Gateways publish with QoS 1 on `quadrapp/{institutionId}/{gatewayHardwareId}/readings`; the backend subscribes to `$share/<group>/quadrapp/+/+/readings`. The broker must only let each gateway publish under its own prefix, because the backend trusts the topic as the gateway identity. Payload:

```json
{"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","sensorId":"sensor-0342","kind":"SPACE","state":"OCCUPIED","ts":"2026-10-09T13:05:12.200Z","battery":82,"rssi":-71}
```

`kind` is `SPACE` (with `state` `FREE` or `OCCUPIED`), `PASS` (with `direction` `IN` or `OUT`) or `HEARTBEAT`; `battery` (0-100) and `rssi` are optional. A message is acknowledged only after it was processed. Malformed messages, readings whose topic does not match the device, and readings that still fail after 3 retries are stored in `sensing.rejected_readings` and acknowledged. If a reading can be neither processed nor stored (for example while the database is down), the backend keeps trying that reading, waiting longer each round up to 30 seconds, and the next readings wait behind it; ingestion resumes on its own when the database is back. A message is never left unacknowledged, because Mosquitto stops delivering after 20 unacknowledged messages and only redelivers them on a new connection. A rejection that can never be stored (a data error, not an outage) is logged and acknowledged; NUL characters in payloads are stored escaped.

### Demo data and simulated sensors

`tools/docker-compose.dev.yml` starts Mailpit and a local Mosquitto (anonymous, development only). `tools/seed/seed.py` creates a demo institution, parking lot and devices through the API, and `tools/simulator/simulator.py` publishes their readings over MQTT. The first platform administrator has to be inserted once with SQL; see [tools/seed/README.md](tools/seed/README.md) and [tools/simulator/README.md](tools/simulator/README.md).

## Database

Each bounded context owns a PostgreSQL schema, with no foreign keys between schemas:

| Module | Schema | Migrations |
| --- | --- | --- |
| IAM | `iam` | `src/main/resources/db/migration/iam` |
| Parking Configuration | `configuration` | `src/main/resources/db/migration/configuration` |
| Parking Sensing | `sensing` | `src/main/resources/db/migration/sensing` |
| Occupancy | `occupancy` | `src/main/resources/db/migration/occupancy` |
| Prediction | `prediction` | `src/main/resources/db/migration/prediction` |
| Shared infrastructure (event publication registry, ShedLock) | `shared` | `src/main/resources/db/migration/shared` |

- Tables are created only by Flyway. Every module runs its own migration and keeps its own `flyway_schema_history` inside its schema (`ModuleSchemaMigrations`).
- Hibernate runs with `ddl-auto=validate`: it checks the JPA mappings against the migrated schema and never changes it.
- To change a table, add a new file `V<n>__<description>.sql` to the module's folder. Never edit a migration that was already applied.
- Each JPA entity declares its module schema in `@Table(schema = "...")`, and native SQL names the schema of every table it uses, because Hibernate validation does not check native queries.
- Upgrade path: a database created before module schemas keeps its tables in `public`. On startup they are moved, with their data, into the module schema and Flyway baselines it at version 1. If only some of a module's tables exist in `public`, or they exist in both schemas, the application refuses to start and names the tables. Every startup also drops any foreign key between a module schema and another schema (CON-02), so a database created with an old hand-written script ends with none.
- A new module adds its schema folder and a `FlywayMigrationInitializer` bean built with `ModuleSchemaMigrations.forSchema(...)`.

## Modules and events

- Module boundaries are checked by Spring Modulith (`ModularityTest`, `ApplicationModules.verify()`): a module may only use another module's `interfaces.acl` facade and its published events (`domain.model.events`). `shared` is an open module.
- Modules publish domain events as Spring application events and consume them with `@ApplicationModuleListener`, which runs after the publishing transaction commits, in a new transaction and on another thread.
- The JDBC event publication registry (`shared.event_publication`) stores each event in the same transaction that produced it (transactional outbox). Nothing is republished on restart (`spring.modulith.events.republish-outstanding-events-on-restart=false`): every redelivery goes through the retry job below and its attempt limit. A publication can still be delivered more than once, so listeners must drop duplicates (for example by `eventId`).
- A publication whose listener failed is delivered again every minute by `EventPublicationMaintenance`, up to `quadrapp.events.retry.max-attempts` deliveries (5); after that it stays FAILED and is logged. A publication a stopped replica left waiting, in progress or resubmitted becomes FAILED after 10 minutes (`spring.modulith.events.staleness.published`, `.processing` and `.resubmitted`; the same job applies them) and is then retried within the same limit. Completed publications are deleted after `quadrapp.events.completed-retention` (7 days). Two replicas delivering the same event at once do not fail: the second one is dropped as a duplicate.
- The backend runs as several replicas, so every `@Scheduled` job must also carry `@SchedulerLock` (ShedLock, table `shared.shedlock`): only one replica runs each execution. `quadrapp.scheduling.enabled=false` turns every scheduled job off (tests use it to call a job directly without a scheduled run racing it).

## Occupancy

- Occupancy owns the state of every space (`occupancy.space_occupancy`: FREE, OCCUPIED or UNKNOWN with its reason) and of every parking lot (`occupancy.lot_occupancy`: counts of the active spaces and the low-availability condition). Both tables use optimistic locking (`version`), and every write locks the parking lot row first, so detections of one lot are applied one at a time.
- Space states change only with the Parking Sensing events (`SensingEventListener`). A detection already applied (same `eventId`) or not after the last applied one changes nothing; an inactive space never changes state and is not counted; a new space starts UNKNOWN. Events that can never apply (unknown parking lot, another institution, a space or access outside the layout) are logged and dropped instead of failing forever.
- The layout comes only from the Parking Configuration facade (`ParkingConfigurationContextFacade.layoutOf`), never from SQL on the `configuration` schema. Occupancy copies it the first time it sees a parking lot, again when the copy is older than `quadrapp.occupancy.layout-max-age` (default `PT1M`), and when a detection names a space or access the copy does not have yet. Occupancy no longer writes the space status into Parking Configuration.
- Low availability starts when the free spaces fall below the lot's `lowAvailabilityPct` (5-30 %, 10 % by default, set when registering the parking lot) and ends when they rise above that value plus 5 points. With no known space (all UNKNOWN) the condition does not change.
- Published events (`occupancy.domain.model.events`): `SpaceMarkedOccupied`, `SpaceMarkedFree`, `SpaceMarkedUnknown` (with `tenantId`), `LowAvailabilityStarted` and `LowAvailabilityEnded`.
- History (`OccupancySchedulers`, each job with ShedLock): one snapshot per zone every minute, kept 8 weeks (the window of the Prediction fallback); vehicle passages (`occupancy.vehicle_passages`) and the deduplication log of sensor events are kept 7 days. An hourly job deletes older rows.
- REST: `GET /api/v1/parking-lots/{id}/occupancy`, `GET /api/v1/zones/{id}/occupancy`, `GET /api/v1/levels/{id}/occupancy`, `POST` and `GET /api/v1/zones/{id}/occupancy-snapshots` keep their routes and responses.
- `GET /api/v1/parking-lots/{id}/spaces/status`: one entry per active space, ordered by `spaceId`, with `spaceId`, `zoneId`, `levelId`, `status` (FREE, OCCUPIED or UNKNOWN), `unknownReason` (only when UNKNOWN), `statusSince` and `lastDetectedAt` (null until the first detection), to overlay on the layout. Any user of the lot's institution can read it, drivers included.
- A resource of another institution answers 404 with the same message as a missing one. Without `from` and `to`, the snapshot history covers the last 24 hours.

### Real-time occupancy (STOMP)

- Endpoint `/ws` (STOMP over WebSocket). The CONNECT frame must carry `Authorization: Bearer <access token>`, the same token as the REST API.
- Subscribe to `/topic/parking-lots/{lotId}/occupancy`; only parking lots of the user's institution are allowed. A refused frame closes the session with an ERROR frame that names the reason.
- Each message holds the zone and parking lot counts, `lowAvailability`, `consolidatedAt` and the spaces of the zone that changed. A quiet zone is sent at once; during a burst (for example a gateway replaying its buffer) at most the last state of each zone is sent per second.
- Allowed browser origins: `quadrapp.realtime.allowed-origins` (default `*`; the token, not a cookie, authenticates). The broker is in memory, so each replica delivers the changes it processed to its own clients.

## Tests

```bash
./mvnw test
```

The integration tests start the whole application against an empty PostgreSQL in Docker and check that every module migrates into its own schema.

## Container image

```bash
docker build -t quadrapp-backend .
```

The `Dockerfile` builds the jar with the Maven wrapper on a JDK 25 image (tests are skipped there) and runs it on a JRE 25 image as a non-root user, with the same environment variables as [Run locally](#run-locally). The project has no Spring Boot Actuator, so the image health check requests `/v3/api-docs` and expects `200`.

## Deployment

The prototype runs on one EC2 instance with Docker Compose (Caddy, the backend, Mosquitto and PostgreSQL). Files, scripts and the step-by-step guide are in [`deploy/`](deploy/README.md).
