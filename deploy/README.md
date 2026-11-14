# Quadrapp deployment (prototype)

One EC2 instance runs the whole prototype with Docker Compose (decision D7b): Caddy (HTTPS and reverse proxy), the backend, Mosquitto 2 (MQTT, TLS on 8883) and PostgreSQL 16. GitHub Actions builds the backend image and pushes it to GitHub Container Registry; the instance pulls it.

```
Internet ──443──▶ caddy ──▶ backend:8080 ──▶ postgres:5432
         └─8883─▶ mosquitto (TLS) ◀── backend (plain 1883, Docker network only)
```

| Service | Image | Published on the host | Data |
| --- | --- | --- | --- |
| `caddy` | `caddy:2.11-alpine` | 443/tcp | `caddy_data` (certificates), `caddy_config` |
| `backend` | `ghcr.io/1asi0728-2620-9056-quadrapp/quadrapp-backend:<BACKEND_TAG>` | none | none |
| `mosquitto` | `eclipse-mosquitto:2.1-alpine` | 8883/tcp | `mosquitto_data` (sessions, queues, credentials) |
| `postgres` | `postgres:16-alpine` | none | `pgdata` |

## Limits of this first version

- **One backend instance with `SPRING_PROFILES_ACTIVE=dev`.** The backend only starts with the `dev` profile: the OTP store and the sensing adapters (`InMemoryOtpChallengeStore`, `InMemoryReadingProcessingAdapter`, `InMemorySensorFeedStateAdapter`) exist only there and keep their state in memory. Two replicas would not share OTP codes or sensor state, so the second replica (QAS-04) comes in Sprint 2 with persistent stores. A backend restart forgets pending OTP codes.
- **No Redis.** D7b lists Redis 7, but nothing in the backend uses Redis today (no dependency, no configuration). It is added when a feature needs it.
- **Single point of failure.** If the instance stops, everything stops. `restart: unless-stopped` only recovers crashed containers. Docker Compose does not restart a container that is only *unhealthy*.
- **No deploy from GitHub Actions yet.** Upgrades are a manual `docker compose pull` (see [Next steps](#next-steps)).

## Operating system: Ubuntu Server 24.04 LTS

Ubuntu 24.04 (arm64) instead of Amazon Linux 2023, because Docker publishes and updates Docker Engine and the Compose plugin for Ubuntu through its own apt repository. On Amazon Linux 2023 the Compose plugin is not packaged and must be installed and updated by hand. The AWS CLI v2 and the SSM agent, preinstalled on Amazon Linux, take one command each on Ubuntu (the SSM agent is already included in the Ubuntu AMI).

## Backend image (CI)

`.github/workflows/backend-image.yml` runs on every push to `main` and `develop`, on tags `v*`, and by hand (`workflow_dispatch`):

1. `test` job: `./mvnw test` on Temurin 25 (Testcontainers uses the runner's Docker).
2. `image` job, only if the tests pass: builds the `Dockerfile` for `linux/arm64` (the t4g instance) and `linux/amd64` (x86_64 machines) and pushes them as one multi-platform image to `ghcr.io/1asi0728-2620-9056-quadrapp/quadrapp-backend` with the tags `sha-<7 chars>`, the branch name (`develop`, `main`) and the git tag. It authenticates with the workflow's `GITHUB_TOKEN`; no repository secret is needed.

The package is private by default. Either make it public (GitHub > organization > Packages > `quadrapp-backend` > Package settings) or log in on the instance with a `read:packages` token (step 3).

## 1. AWS resources

Create these once, in the same region (for example `us-east-1`):

| Resource | Setting |
| --- | --- |
| EC2 instance | `t4g.small` (AWS Graviton, arm64, 2 vCPU, 2 GiB), AMI Ubuntu Server 24.04 LTS (arm64), gp3 root volume of 30 GB, IMDSv2 required. Decision D7b named `c7i-flex.large` (x86_64); the team chose the t4g.small instead. The memory limits in `docker-compose.yml` add up to about 1.7 GB to fit it |
| Elastic IP | Associated with the instance, so DNS survives stop and start |
| Security group | Inbound 443/tcp from `0.0.0.0/0` and `::/0`; 8883/tcp from `0.0.0.0/0` (or only the campus IP of the gateways); no port 22 (use Session Manager) or 22/tcp from your IP `/32` only. Port 80 stays closed: Caddy validates with the TLS-ALPN challenge on 443 |
| IAM role of the instance | `AmazonSSMManagedInstanceCore` (Session Manager), plus an inline policy with `s3:PutObject`, `s3:GetObject` and `s3:ListBucket` on the backup bucket |
| S3 bucket | Private, for `pg_dump` files, with a lifecycle rule that expires objects under `postgres/` after 30 days |
| Budget | An AWS Budgets alert on the credit, so a forgotten instance does not use it up |

## 2. DNS

Create two A records pointing to the Elastic IP, for example:

| Name | Use |
| --- | --- |
| `api.<domain>` | `API_DOMAIN`: REST API, `/v3/api-docs`, `/swagger-ui`, WebSocket `/ws` |
| `mqtt.<domain>` | `MQTT_DOMAIN`: Mosquitto on 8883. Caddy obtains its certificate and Mosquitto uses it |

They must be two different names. Check them with `dig +short api.<domain>` before the first start; Let's Encrypt fails while DNS does not resolve.

## 3. Prepare the instance

Connect with Session Manager (`aws ssm start-session --target <instance-id>`) or SSH, then:

```bash
# Docker Engine and the Compose plugin from Docker's apt repository
sudo apt-get update
sudo apt-get install -y ca-certificates curl unzip
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker ubuntu   # log out and in again

# AWS CLI v2 (uses the instance role, no keys)
curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-$(uname -m).zip" -o /tmp/awscliv2.zip   # aarch64 on t4g
unzip -q /tmp/awscliv2.zip -d /tmp && sudo /tmp/aws/install && rm -rf /tmp/aws /tmp/awscliv2.zip

# 2 GB swap file: a safety margin for the 2 GiB of the t4g.small, not working memory
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Deployment files
git clone --depth 1 --branch develop https://github.com/1ASI0728-2620-9056-Quadrapp/quadrapp-backend.git
cd quadrapp-backend/deploy
```

If the GHCR package is private, log in once with a GitHub personal access token that has only `read:packages`:

```bash
docker login ghcr.io -u <github-user>
```

## 4. Configure

```bash
cp env.example .env
chmod 600 .env
nano .env
```

Replace every `change-me`. Generate secrets on the instance with `openssl rand -base64 48` (JWT key) and `openssl rand -base64 32` (pepper and passwords). `.env` is ignored by git; never commit it.

| Variable | Notes |
| --- | --- |
| `API_DOMAIN`, `MQTT_DOMAIN`, `ACME_EMAIL` | Host names from step 2 and the Let's Encrypt contact |
| `BACKEND_TAG` | `develop`, `main`, a git tag, or `sha-<7 chars>` to pin one build |
| `POSTGRES_PASSWORD` | Also used as `DB_PASSWORD` of the backend |
| `JWT_SECRET_BASE64`, `OTP_HASH_PEPPER_BASE64` | Changing them signs everyone out and invalidates pending codes |
| `MAIL_*` | Brevo SMTP relay: `smtp-relay.brevo.com`, port 587, STARTTLS. `MAIL_USERNAME` is the Brevo SMTP login and `MAIL_PASSWORD` the SMTP key. `MAIL_FROM` must be a sender verified in Brevo |
| `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | `true` for Brevo. `application.properties` sets `spring.mail.properties.mail.smtp.auth` and `.starttls.enable` to `false` for Mailpit; Compose overrides them with `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` and `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` |
| `MQTT_ADMIN_*` | Administrator of Mosquitto's dynamic security plugin, used only by the scripts |
| `MQTT_BACKEND_*`, `QUADRAPP_MQTT_*` | Become `quadrapp.mqtt.*` of the backend (`enabled`, `url`, `username`, `password`, `client-id`, `shared-group`) through Spring relaxed binding |
| `BACKUP_S3_URI` | `s3://<bucket>/postgres` |

## 5. First start

```bash
# 1. Caddy obtains the certificates of API_DOMAIN and MQTT_DOMAIN (about a minute)
docker compose up -d caddy
docker compose logs -f caddy   # wait for "certificate obtained successfully" twice, then Ctrl+C

# 2. Mosquitto: dynamic security file, admin client and backend client
scripts/init-mqtt.sh

# 3. Everything else
docker compose up -d
docker compose ps              # backend, mosquitto and postgres become "healthy"

# 4. Check from your computer
curl -s -o /dev/null -w '%{http_code}\n' https://api.<domain>/v3/api-docs   # 200
```

Flyway creates the schemas on the first backend start. `init-mqtt.sh` can run again: it keeps the existing file and sets the backend password from `.env`.

## 6. Gateways (MQTT credentials)

Each gateway gets its own client, which can only publish to `quadrapp/<tenantId>/<gatewayId>/#` (D7a point 8). The backend client can only subscribe (`quadrapp/#` and `$share/<group>/quadrapp/#`).

```bash
scripts/add-gateway.sh <tenantId> <gatewayId>       # prints host, port, client id, username and a random password
scripts/remove-gateway.sh <tenantId> <gatewayId>    # revokes it
```

The gateway connects to `MQTT_DOMAIN:8883` with TLS (the Let's Encrypt chain is in the system CA bundle), client id and username `gw-<tenantId>-<gatewayId>`. Ids may only contain letters, digits, `-` and `_`.

Broker settings (`mosquitto/mosquitto.conf`, D7a points 4 and 5):

- The SQLite persistence plugin writes every change to the `mosquitto_data` volume, instead of the default snapshot every 30 minutes (`autosave_interval 1800`). The built-in snapshot cannot be combined with the plugin, so there is no `autosave_interval`. A queued QoS 1 message survived a `docker kill -s KILL` of the broker in local tests.
- `max_queued_messages 100000` per client (default 1,000) and persistent sessions kept for 7 days without reconnecting.

## 7. Scheduled jobs (cron)

```bash
crontab -e
```

```cron
# Daily pg_dump to S3 at 08:00 UTC (03:00 in Lima)
0 8 * * * /home/ubuntu/quadrapp-backend/deploy/scripts/backup-db.sh >> /home/ubuntu/quadrapp-backup.log 2>&1
# Restart Mosquitto when Caddy has renewed the MQTT certificate
30 8 * * * /home/ubuntu/quadrapp-backend/deploy/scripts/renew-mqtt-cert.sh >> /home/ubuntu/quadrapp-cert.log 2>&1
```

Caddy renews certificates by itself. Mosquitto only reads its certificate at start (`mosquitto/start.sh` copies it from Caddy's volume), so `renew-mqtt-cert.sh` restarts it when the certificate changed. Gateways reconnect and keep their sessions.

## 8. Upgrade

```bash
cd ~/quadrapp-backend && git pull           # deployment files
cd deploy
docker compose pull backend
docker compose up -d backend                # recreates only what changed
docker compose ps
```

Rollback: set `BACKEND_TAG=sha-<previous 7 chars>` in `.env` and run `docker compose up -d backend`. Migrations already applied are not reverted; a migration that breaks the previous version needs a restore.

## 9. Backup and restore

```bash
scripts/backup-db.sh                                           # now, outside cron
aws s3 ls s3://<bucket>/postgres/                              # available dumps
scripts/restore-db.sh s3://<bucket>/postgres/<file>.dump       # asks to type the database name
```

`backup-db.sh` writes a `pg_dump --format=custom` file, checks it with `pg_restore --list` and uploads it; a failed dump is never uploaded. `restore-db.sh` stops the backend, restores with `--clean --if-exists` in one transaction (on error, the database stays as it was) and starts the backend again.

The volumes `mosquitto_data` (sessions and gateway credentials) and `caddy_data` (certificates) are not backed up; take an EBS snapshot of the instance for them.

## 10. Logs and operations

| Task | Command |
| --- | --- |
| Follow the backend log | `docker compose logs -f --tail=200 backend` |
| Requests through Caddy | `docker compose logs -f caddy` |
| Broker log | `docker compose logs -f mosquitto` |
| State and health | `docker compose ps` |
| Memory per container | `docker stats --no-stream` |
| SQL console | `docker compose exec postgres psql -U quadrapp -d quadrapp` |
| Pause the prototype | `docker compose stop` (or stop the instance; the Elastic IP is billed while it is stopped) |

Logs rotate at 10 MB × 5 files per container. Memory limits: backend 1 GB, PostgreSQL 768 MB, Mosquitto 256 MB, Caddy 128 MB (measured idle: about 370 MB, 46 MB, 4 MB and 26 MB).

## Test the stack on a laptop

`TLS_MODE=internal` makes Caddy use its own local CA, so no DNS or open ports are needed:

```bash
cp env.example test.env   # fill the change-me values, then set:
# API_DOMAIN=api.quadrapp.localhost  MQTT_DOMAIN=mqtt.quadrapp.localhost  TLS_MODE=internal
# HTTPS_PORT=18443  MQTTS_PORT=18883  COMPOSE_PROJECT_NAME=quadrapp-test
# BACKEND_IMAGE=quadrapp-backend  BACKEND_TAG=local   (after: docker build -t quadrapp-backend:local ..)
docker compose --env-file test.env up -d caddy
ENV_FILE=$PWD/test.env scripts/init-mqtt.sh
docker compose --env-file test.env up -d
docker compose --env-file test.env exec -T caddy cat /data/caddy/pki/authorities/local/root.crt > caddy-root.crt
curl --cacert caddy-root.crt --resolve api.quadrapp.localhost:18443:127.0.0.1 https://api.quadrapp.localhost:18443/v3/api-docs
docker compose --env-file test.env down -v   # removes the test volumes
```

## Next steps

These need the AWS account and are not automated yet:

1. **Deploy from GitHub Actions.** Add an AWS IAM role trusted by GitHub's OIDC provider (no stored keys) that may only run `ssm:SendCommand` with `AWS-RunShellScript` on this instance. A job after `image` then runs `cd ~/quadrapp-backend/deploy && docker compose pull backend && docker compose up -d backend` on the instance through SSM.
2. **Secrets in SSM Parameter Store.** Store the `.env` values as `SecureString` parameters under `/quadrapp/prod/`, give the instance role `ssm:GetParametersByPath` on that path, and render `.env` from them before `docker compose up` instead of editing it by hand.
3. **Second backend replica** once the OTP store and the sensing adapters are persistent (Sprint 2): remove `SPRING_PROFILES_ACTIVE=dev`, add a second service, and let Caddy balance with `reverse_proxy backend-1:8080 backend-2:8080`.
