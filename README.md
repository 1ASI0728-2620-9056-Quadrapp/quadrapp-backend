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

