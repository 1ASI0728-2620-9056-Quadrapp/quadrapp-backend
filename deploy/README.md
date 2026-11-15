# Quadrapp deployment

One EC2 instance (`t4g.small`, arm64, Ubuntu 24.04) runs the whole prototype with Docker Compose. The full configuration (AWS resources, security group, secrets, pipeline and costs) is documented in the project report, section 7.1.4.

```
Internet ──443──▶ caddy ──▶ backend:8080 ──▶ postgres:5432
         └─8883─▶ mosquitto (TLS) ◀── backend (plain 1883, Docker network only)
```

| Service | Image | Published | Memory limit |
| --- | --- | --- | --- |
| `caddy` | `caddy:2.11-alpine` | 443/tcp | 128 MB |
| `backend` | `ghcr.io/1asi0728-2620-9056-quadrapp/quadrapp-backend:<BACKEND_TAG>` | none | 896 MB |
| `mosquitto` | `eclipse-mosquitto:2.1-alpine` | 8883/tcp | 128 MB |
| `postgres` | `postgres:16-alpine` | none | 512 MB |

Limits of this first version: one backend instance with `SPRING_PROFILES_ACTIVE=dev` (the OTP store and the sensing state live in memory, so a restart forgets pending codes), no Redis, and a single point of failure.

## Deploy

Every push to `main` deploys by itself (`.github/workflows/backend-image.yml`): tests, a multi-platform image `sha-<7 chars>` in GHCR, then GitHub OIDC → AWS role `quadrapp-github-deploy` → SSM command on the instance, which runs `scripts/deploy.sh sha-<7 chars>`:

1. writes the `SecureString` parameters under `/quadrapp/prod/` into `.env`;
2. pulls the image (a failed pull changes nothing) and runs `docker compose up -d`;
3. waits up to 180 s for `https://<API_DOMAIN>/v3/api-docs` = 200, otherwise puts the previous image back and the job fails.

`develop` only builds its image. Redeploy the current `main` with Actions → backend-image → Run workflow on `main`.

- The role trusts only `repo:1ASI0728-2620-9056-Quadrapp@328915374/quadrapp-backend@1450555298:ref:refs/heads/main` (immutable ids).
- Rollback only reverts the image: Flyway migrations stay applied, so a migration must keep working with the previous version.
- To change a secret: `aws ssm put-parameter --name /quadrapp/prod/<KEY> --type SecureString --overwrite --value '<value>'`, then redeploy. Editing `.env` alone is overwritten on the next deploy.

By hand on the instance (the checkout stays detached at the last deployed commit):

```bash
cd ~/quadrapp-backend && git fetch && git checkout <commit>
sudo deploy/scripts/deploy.sh sha-<7 chars>    # or develop, main, a git tag
```

## First setup of a new instance

1. Install Docker Engine with the Compose plugin and the AWS CLI v2, add 2 GB of swap, and clone this repository.
2. `cp env.example .env && chmod 600 .env`, then fill it (variables are described in `env.example`).
3. `docker compose up -d caddy` and wait for both certificates (Let's Encrypt, TLS-ALPN on 443).
4. `scripts/init-mqtt.sh` (Mosquitto admin and backend clients), then `docker compose up -d`.
5. Create the first platform administrator with the SQL in `tools/seed/README.md`.

## Operations

| Task | Command (from `deploy/`) |
| --- | --- |
| Gateway credential | `scripts/add-gateway.sh <tenantId> <gatewayId>` (client id and username `gw-<tenantId>-<gatewayId>`, publishes only to `quadrapp/<tenantId>/<gatewayId>/#`); revoke with `scripts/remove-gateway.sh` |
| Backup to S3 (by hand) | `scripts/backup-db.sh`; restore with `scripts/restore-db.sh s3://<bucket>/postgres/<file>.dump` |
| Restart Mosquitto after Caddy renews its certificate | `scripts/renew-mqtt-cert.sh` |
| Logs | `docker compose logs -f --tail=200 backend` (or `caddy`, `mosquitto`) |
| State and memory | `docker compose ps`, `docker stats --no-stream` |
| SQL console | `docker compose exec postgres psql -U quadrapp -d quadrapp` |

## Test the stack on a laptop

Set `TLS_MODE=internal` in a copy of `env.example` (with `API_DOMAIN=api.quadrapp.localhost`, `MQTT_DOMAIN=mqtt.quadrapp.localhost`, other ports and `COMPOSE_PROJECT_NAME`) and run `docker compose --env-file test.env up -d`: Caddy uses its own local CA, so no DNS or open ports are needed.
