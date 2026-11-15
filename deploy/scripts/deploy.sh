#!/usr/bin/env bash
# Deploys one backend image tag on the instance and rolls back when it does not become ready.
# The deploy job of .github/workflows/backend-image.yml runs it through SSM (as root) after
# checking out the commit of that image; it also works by hand on the instance.
# Usage: scripts/deploy.sh <image tag>   (for example sha-7207644)
#  1. Writes every SecureString parameter under /quadrapp/prod/ (SSM Parameter Store) into
#     .env, one key per parameter name. Keys without a parameter (domains, ACME_EMAIL...)
#     keep the value they have in .env.
#  2. Pulls the image of the tag. A failed pull stops here: the running stack is not touched.
#  3. Sets BACKEND_TAG, runs docker compose up -d and waits until
#     https://API_DOMAIN/v3/api-docs answers 200 through Caddy.
#  4. Otherwise it restores the previous BACKEND_TAG, starts it again and exits with 1.
# Only the image is rolled back: Flyway migrations stay applied and the deploy/ files stay at
# the new commit, so migrations must keep working with the previous version.
set -euo pipefail
# SSM and cron run with a short PATH, but the AWS CLI v2 installer puts aws in /usr/local/bin.
export PATH="/usr/local/bin:/usr/bin:/bin:${PATH:-}"
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

tag="${1:-}"
[[ "$tag" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]] || die "Usage: scripts/deploy.sh <image tag>, for example sha-7207644."
params_path="${PARAMS_PATH:-/quadrapp/prod}"
region="${AWS_REGION:-us-east-1}"
ready_timeout="${READY_TIMEOUT_SECONDS:-180}"
command -v aws >/dev/null || die "The AWS CLI is not installed."

# One deploy at a time: a newer workflow run can start while an older SSM command still runs.
exec 9>/tmp/quadrapp-deploy.lock
flock -w 900 9 || die "Another deploy is still running."

# Replaces KEY=value in the env file (or appends it) without printing the value. The value is
# passed through the environment, so awk does not interpret backslashes in it.
set_env_value() {
	local tmp
	tmp=$(mktemp "$ENV_FILE.XXXXXX")
	KEY="$1" VALUE="$2" awk '
		BEGIN { key = ENVIRON["KEY"]; value = ENVIRON["VALUE"]; done = 0 }
		index($0, key "=") == 1 { if (!done) print key "=" value; done = 1; next }
		{ print }
		END { if (!done) print key "=" value }
	' "$ENV_FILE" >"$tmp"
	chown --reference="$ENV_FILE" "$tmp"
	chmod 600 "$tmp"
	mv "$tmp" "$ENV_FILE"
}

sync_secrets() {
	local names name key value count=0
	names=$(aws ssm get-parameters-by-path --region "$region" --path "$params_path" \
		--query 'Parameters[].Name' --output text)
	for name in $names; do
		key="${name##*/}"
		[[ "$key" =~ ^[A-Z][A-Z0-9_]*$ ]] || die "Unexpected parameter name: $name"
		value=$(aws ssm get-parameter --region "$region" --name "$name" --with-decryption \
			--query Parameter.Value --output text)
		set_env_value "$key" "$value"
		count=$((count + 1))
	done
	[ "$count" -gt 0 ] || die "No parameters under $params_path in $region."
	echo "Wrote $count secrets from $params_path into .env."
}

# Waits until the API answers 200 through Caddy, as a client on the internet would see it.
wait_until_ready() {
	local timeout="$1" domain port deadline code
	domain=$(require_value API_DOMAIN)
	port=$(env_value HTTPS_PORT)
	port=${port:-443}
	deadline=$((SECONDS + timeout))
	while [ "$SECONDS" -lt "$deadline" ]; do
		code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 \
			--resolve "$domain:$port:127.0.0.1" "https://$domain:$port/v3/api-docs" || true)
		[ "$code" = "200" ] && return 0
		sleep 5
	done
	return 1
}

previous=$(env_value BACKEND_TAG)
sync_secrets

echo "Pulling the backend image $tag."
BACKEND_TAG="$tag" compose pull --quiet backend || die "Could not pull $tag. Nothing was changed."

set_env_value BACKEND_TAG "$tag"
compose up -d
if wait_until_ready "$ready_timeout"; then
	echo "Deployed $tag (previous: ${previous:-none})."
	exit 0
fi

echo "$tag did not answer within ${ready_timeout}s. Last backend logs:" >&2
compose logs --tail 40 backend >&2 || true
if [ -z "$previous" ] || [ "$previous" = "$tag" ]; then
	die "There is no previous tag to roll back to."
fi

echo "Rolling back to $previous." >&2
set_env_value BACKEND_TAG "$previous"
compose up -d
# The previous version gets at least 180 s, even when READY_TIMEOUT_SECONDS is shorter.
if wait_until_ready "$((ready_timeout > 180 ? ready_timeout : 180))"; then
	die "Rolled back to $previous; $tag was not deployed."
fi
die "Rolled back to $previous, but it does not answer either. Check: docker compose ps"
