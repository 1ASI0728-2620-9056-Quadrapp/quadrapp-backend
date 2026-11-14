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
- The backend runs as several replicas, so every `@Scheduled` job must also carry `@SchedulerLock` (ShedLock, table `shared.shedlock`): only one replica runs each execution.

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
