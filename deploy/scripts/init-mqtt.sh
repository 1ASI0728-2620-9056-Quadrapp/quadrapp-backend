#!/usr/bin/env bash
# Prepares Mosquitto's dynamic security plugin. Safe to run again.
#  1. On the first run, creates /mosquitto/data/dynamic-security.json with the admin client
#     (MQTT_ADMIN_USERNAME / MQTT_ADMIN_PASSWORD).
#  2. Starts the broker and creates the backend client (MQTT_BACKEND_USERNAME /
#     MQTT_BACKEND_PASSWORD) with the role backend-subscriber, which can only subscribe to
#     quadrapp/# (also through a shared subscription, $share/<group>/quadrapp/#).
#     When the client already exists, its password is set again from the env file.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

admin_user=$(require_value MQTT_ADMIN_USERNAME)
admin_password=$(require_value MQTT_ADMIN_PASSWORD)
backend_user=$(require_value MQTT_BACKEND_USERNAME)
backend_password=$(require_value MQTT_BACKEND_PASSWORD)
backend_role=backend-subscriber

for value in "$admin_password" "$backend_password"; do
	[ "$value" != "change-me" ] || die "Set real MQTT passwords in .env; change-me is only the template value."
done

if compose run --rm --no-deps -T --entrypoint test mosquitto -f /mosquitto/data/dynamic-security.json; then
	echo "The dynamic security configuration already exists; keeping it."
else
	compose run --rm --no-deps -T --entrypoint mosquitto_ctrl mosquitto \
		dynsec init /mosquitto/data/dynamic-security.json "$admin_user" "$admin_password"
fi

compose up -d mosquitto
wait_for_mosquitto

if ! dynsec getRole "$backend_role" >/dev/null 2>&1; then
	dynsec createRole "$backend_role"
	dynsec addRoleACL "$backend_role" subscribePattern 'quadrapp/#' allow
	# shellcheck disable=SC2016 # literal $share topic filter
	dynsec addRoleACL "$backend_role" subscribePattern '$share/+/quadrapp/#' allow
	echo "Created the role $backend_role."
fi

if dynsec getClient "$backend_user" >/dev/null 2>&1; then
	dynsec setClientPassword "$backend_user" "$backend_password"
	echo "The client $backend_user already exists; its password was set from the env file."
else
	dynsec createClient "$backend_user" -p "$backend_password"
	dynsec addClientRole "$backend_user" "$backend_role"
	echo "Created the client $backend_user with the role $backend_role."
fi
