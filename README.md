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
- The JDBC event publication registry (`shared.event_publication`) stores each event in the same transaction that produced it (transactional outbox). Publications not completed when the application stops are published again on restart, so listeners must drop duplicates (for example by `eventId`).
- A publication whose listener failed is delivered again every minute by `EventPublicationMaintenance` (not only on restart), up to `quadrapp.events.retry.max-attempts` deliveries (5); after that it stays FAILED and is logged. A publication left in progress by a stopped replica becomes FAILED after 10 minutes (`spring.modulith.events.staleness.processing`). Completed publications are deleted after `quadrapp.events.completed-retention` (7 days). Two replicas delivering the same event at once do not fail: the second one is dropped as a duplicate.
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
