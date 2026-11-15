#!/usr/bin/env bash
# Creates the MQTT credential of one gateway (decision D7a point 8).
# Usage: scripts/add-gateway.sh <tenantId> <gatewayId> [password]
# The gateway can only publish to quadrapp/<tenantId>/<gatewayId>/# and must connect with
# the client id gw-<tenantId>-<gatewayId>. Without a password, a random one is generated.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

[ $# -ge 2 ] && [ $# -le 3 ] || die "Usage: $0 <tenantId> <gatewayId> [password]"
tenant_id=$1
gateway_id=$2
require_id tenantId "$tenant_id"
require_id gatewayId "$gateway_id"
password=${3:-$(head -c 32 /dev/urandom | base64 | tr -d '/+=\n' | cut -c 1-32)}

name="gw-${tenant_id}-${gateway_id}"
topic="quadrapp/${tenant_id}/${gateway_id}/#"

if dynsec getClient "$name" >/dev/null 2>&1; then
	die "The client $name already exists. Remove it first with scripts/remove-gateway.sh $tenant_id $gateway_id."
fi

if ! dynsec getRole "$name" >/dev/null 2>&1; then
	dynsec createRole "$name"
	dynsec addRoleACL "$name" publishClientSend "$topic" allow
fi
dynsec createClient "$name" -i "$name" -p "$password"
dynsec addClientRole "$name" "$name"

cat <<EOF
Gateway credential created. Store it in the gateway configuration; it is not shown again.
  host:      $(env_value MQTT_DOMAIN)
  port:      $(env_value MQTTS_PORT) (MQTT over TLS, Let's Encrypt certificate)
  client id: $name
  username:  $name
  password:  $password
  publishes: $topic
EOF
