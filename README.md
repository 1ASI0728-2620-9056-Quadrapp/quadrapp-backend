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

- Tables are created only by Flyway. Every module runs its own migration and keeps its own `flyway_schema_history` inside its schema (`ModuleSchemaMigrations`).
- Hibernate runs with `ddl-auto=validate`: it checks the JPA mappings against the migrated schema and never changes it.
- To change a table, add a new file `V<n>__<description>.sql` to the module's folder. Never edit a migration that was already applied.
- Each JPA entity declares its module schema in `@Table(schema = "...")`.
- Upgrade path: a database created before module schemas keeps its tables in `public`. On startup they are moved, with their data, into the module schema and Flyway baselines it at version 1. If only some of a module's tables exist in `public`, or they exist in both schemas, the application refuses to start and names the tables.
- A new module adds its schema folder and a `FlywayMigrationInitializer` bean built with `ModuleSchemaMigrations.forSchema(...)`.

## Tests

```bash
./mvnw test
```

The integration tests start the whole application against an empty PostgreSQL in Docker and check that every module migrates into its own schema.
