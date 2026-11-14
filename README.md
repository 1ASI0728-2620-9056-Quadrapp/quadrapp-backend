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

## Parking Configuration: campus events

- `POST /api/v1/calendar-events` schedules an event and `POST /api/v1/calendar-events/{id}/cancellation` cancels it (US35, scenario 3): 200 with the cancelled event (`cancelledAt` set), 409 if it was already cancelled, 404 for a missing event or one of another institution, 403 without the institution write permission. A cancelled event is not scheduled again; register a new one.
- Published events (`parkingconfiguration.domain.model.events`), stored in the same transaction as the change: `CampusEventProvided` (campusEventId, tenantId, parkingLotId, startAt, endAt, impact) and `CampusEventCancelled` (same fields without impact). A null `parkingLotId` means every parking lot of the institution.
- `ParkingConfigurationContextFacade.activeParkingLotIds()` and `predictionProfileOf(lotId, from, to)` are internal reads for system jobs and listeners (they do not check the caller) and never back a REST endpoint.

## Occupancy

- Occupancy owns the state of every space (`occupancy.space_occupancy`: FREE, OCCUPIED or UNKNOWN with its reason) and of every parking lot (`occupancy.lot_occupancy`: counts of the active spaces and the low-availability condition). Both tables use optimistic locking (`version`), and every write locks the parking lot row first, so detections of one lot are applied one at a time.
- Space states change only with the Parking Sensing events (`SensingEventListener`). A detection already applied (same `eventId`) or not after the last applied one changes nothing; an inactive space never changes state and is not counted; a new space starts UNKNOWN. Events that can never apply (unknown parking lot, another institution, a space or access outside the layout) are logged and dropped instead of failing forever.
- The layout comes only from the Parking Configuration facade (`ParkingConfigurationContextFacade.layoutOf`), never from SQL on the `configuration` schema. Occupancy copies it the first time it sees a parking lot, again when the copy is older than `quadrapp.occupancy.layout-max-age` (default `PT1M`), and when a detection names a space or access the copy does not have yet. Occupancy no longer writes the space status into Parking Configuration.
- Low availability starts when the free spaces fall below the lot's `lowAvailabilityPct` (5-30 %, 10 % by default, set when registering the parking lot) and ends when they rise above that value plus 5 points. With no known space (all UNKNOWN) the condition does not change.
- Published events (`occupancy.domain.model.events`): `SpaceMarkedOccupied`, `SpaceMarkedFree`, `SpaceMarkedUnknown` (with `tenantId`), `LowAvailabilityStarted` and `LowAvailabilityEnded`.
- History (`OccupancySchedulers`, each job with ShedLock): one snapshot per zone every minute, kept 8 weeks (the window of the Prediction fallback); vehicle passages (`occupancy.vehicle_passages`) and the deduplication log of sensor events are kept 7 days. An hourly job deletes older rows.
- Prediction reads that history only through the facade: `lotOccupancyAt(lotId, at, tolerance)` (lot totals of the snapshot closest to a moment) and `weeklySlotAverage(lotId, slotStart, slotLength, weeks)` (average occupancy of the same weekday and slot over up to 8 past weeks, with how many weeks had data). Both are bounded queries on the `(parking_lot_id, taken_at)` index; occupancy is occupied / (free + occupied), so unknown spaces never count.
- REST: `GET /api/v1/parking-lots/{id}/occupancy`, `GET /api/v1/zones/{id}/occupancy`, `GET /api/v1/levels/{id}/occupancy`, `POST` and `GET /api/v1/zones/{id}/occupancy-snapshots` keep their routes and responses. A resource of another institution answers 404 with the same message as a missing one. Without `from` and `to`, the snapshot history covers the last 24 hours.

### Real-time occupancy (STOMP)

- Endpoint `/ws` (STOMP over WebSocket). The CONNECT frame must carry `Authorization: Bearer <access token>`, the same token as the REST API.
- Subscribe to `/topic/parking-lots/{lotId}/occupancy`; only parking lots of the user's institution are allowed. A refused frame closes the session with an ERROR frame that names the reason.
- Each message holds the zone and parking lot counts, `lowAvailability`, `consolidatedAt` and the spaces of the zone that changed. A quiet zone is sent at once; during a burst (for example a gateway replaying its buffer) at most the last state of each zone is sent per second.
- Allowed browser origins: `quadrapp.realtime.allowed-origins` (default `*`; the token, not a cookie, authenticates). The broker is in memory, so each replica delivers the changes it processed to its own clients.

## Prediction & Advisory

- Forecasts are generated in runs. `ForecastScheduler` runs every 5 minutes (ShedLock `forecast-runs`) for every active parking lot; registering or cancelling a campus event that overlaps the next 60 minutes regenerates the forecasts of the affected lots (`CampusEventListener`, trigger `CAMPUS_EVENT`). Each forecast stores `runId`, `runSlot` (start of the 5-minute slot) and `trigger` (`SCHEDULED` or `CAMPUS_EVENT`; null for forecasts saved before runs existed). The database allows one forecast per lot, horizon and run, and one `SCHEDULED` run per lot, horizon and slot, so two replicas never save the same run. A daily job (ShedLock `forecast-purge`) deletes forecasts older than 7 days.
- Model (D2, D8): the model is trained in Python and exported to ONNX; the backend runs it in process with ONNX Runtime for Java (`com.microsoft.onnxruntime:onnxruntime`, which ships the linux-x64 and linux-aarch64 native libraries). Put `model-vN.onnx` and its metadata `model-vN.json` in `quadrapp.prediction.model.directory` (env `PREDICTION_MODEL_DIR`; empty by default). The highest N with both files is used, and the directory is checked again every `quadrapp.prediction.model.reload-interval` (default `PT1M`), so a new version loads without a restart. Metadata: `version` (stored as `modelVersion`), `features` (input order, any of `occupancy_pct`, `minute_of_day`, `day_of_week`, `campus_event`; the time features use `quadrapp.prediction.time-zone`, default `America/Lima`), `outputs` (output name per horizon `15`, `30`, `45`, `60`; each `[N, 1]` float) and `maeByHorizon`. Confidence = `1 - MAE / 25` clamped to [0, 1]; level HIGH from 0.8, MEDIUM from 0.5. A test model and the script that generated it are in `src/test/resources/prediction/model`.
- Fallback (rule 6), with method `FALLBACK` and confidence LOW, when no model is loaded, it fails, it does not return a horizon or every space is UNKNOWN: the average occupancy of the same weekday and 15-minute slot over the last 8 weeks (Occupancy's minute snapshots, `modelVersion` `weekly-slot-average-v1`); with less than 2 weeks of history for that slot, the current occupancy (`persistence-v1`). With no history and no known space no forecast is generated (`POST` answers 409). Prediction never calls Analytics.
- Evaluation (every minute, ShedLock `forecast-evaluation`): a forecast is compared with the lot snapshot closest to its `targetAt` (within 5 minutes), each one in its own transaction. One that cannot be observed (no snapshot, no known space) or whose evaluation fails is marked evaluated without an observation, so it never blocks the queue.
- REST: `POST /api/v1/parking-lots/{id}/forecasts` returns the scheduled run of the current slot, generating it first if needed (201); `GET /api/v1/parking-lots/{id}/forecasts` (latest per horizon) and `GET .../forecasts/metrics` keep their routes. Forecast responses only gained `runId`, `runSlot` and `trigger`; `modelVersion` names the model or fallback.
- `POST /api/v1/arrival-advices` with exactly `{"lotId": 7, "etaMinutes": 20}` (integer 0 to 120; any other field is 400) returns `etaMinutes`, `probabilityOfSpace`, `category` (HIGH, LIMITED, LOW), `confidence` (`value`, `level`), `horizonUsed`, `generatedAt` and `serverTime`. It uses the smallest valid horizon that covers the ETA (otherwise the longest, one confidence level lower); a FALLBACK forecast gives LOW confidence and at most LIMITED. Nothing is stored. 404 for a lot of another institution or when no forecast is valid.
- STOMP: after each run, `/topic/parking-lots/{lotId}/forecasts` receives `parkingLotId`, `runId`, `trigger`, `generatedAt` and the forecasts of the run (same `/ws` endpoint, token and lot check as the occupancy topic).

## Tests

```bash
./mvnw test
```

The integration tests start the whole application against an empty PostgreSQL in Docker and check that every module migrates into its own schema.
