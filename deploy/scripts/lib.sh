# Shared helpers of deploy/scripts. Sourced by the other scripts, not run directly.
# ENV_FILE overrides the default deploy/.env (useful to test the stack elsewhere).

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env}"

die() {
	echo "Error: $*" >&2
	exit 1
}

[ -f "$ENV_FILE" ] || die "$ENV_FILE does not exist. Copy env.example to .env and fill it in."

compose() {
	docker compose --project-directory "$DEPLOY_DIR" -f "$DEPLOY_DIR/docker-compose.yml" --env-file "$ENV_FILE" "$@"
}

# Prints the value of a key in the env file (last occurrence, surrounding quotes removed).
env_value() {
	local value
	value=$(sed -n "s/^$1=//p" "$ENV_FILE" | tail -n 1)
	value="${value%\"}"
	value="${value#\"}"
	value="${value%\'}"
	value="${value#\'}"
	printf '%s' "$value"
}

# Like env_value, but fails when the key is missing or empty.
require_value() {
	local value
	value=$(env_value "$1")
	[ -n "$value" ] || die "$1 is not set in $ENV_FILE"
	printf '%s' "$value"
}

# Runs one command of the Mosquitto dynamic security plugin as the admin client, over the
# plain listener inside the container. mosquitto_ctrl exits with 0 even when the command
# fails, so its output is checked for "Error".
dynsec() {
	local output
	output=$(compose exec -T mosquitto mosquitto_ctrl -h 127.0.0.1 -p 1883 \
		-u "$(require_value MQTT_ADMIN_USERNAME)" -P "$(require_value MQTT_ADMIN_PASSWORD)" \
		dynsec "$@" 2>&1 | grep -v -e 'without encryption' -e 'visible on the network' -e '^$' || true)
	if printf '%s' "$output" | grep -q 'Error'; then
		printf '%s\n' "$output" >&2
		return 1
	fi
	if [ -n "$output" ]; then
		printf '%s\n' "$output"
	fi
}

# Waits until the broker accepts connections on its internal listener.
wait_for_mosquitto() {
	local attempt
	for attempt in $(seq 1 40); do
		if compose exec -T mosquitto nc -z 127.0.0.1 1883 >/dev/null 2>&1; then
			return 0
		fi
		sleep 5
	done
	die "Mosquitto did not start. Check: docker compose logs mosquitto (it waits for Caddy's certificate of MQTT_DOMAIN)."
}

# A tenant or gateway id becomes part of a topic and a username, so MQTT wildcards and
# separators (+, #, /) are rejected.
require_id() {
	[[ "$2" =~ ^[A-Za-z0-9_-]{1,64}$ ]] || die "$1 must have 1 to 64 letters, digits, '-' or '_' (got '$2')."
}
