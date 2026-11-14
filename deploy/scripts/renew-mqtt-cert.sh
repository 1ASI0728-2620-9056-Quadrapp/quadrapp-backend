#!/usr/bin/env bash
# Caddy renews the certificate of MQTT_DOMAIN by itself; Mosquitto only reads it at start.
# This script restarts Mosquitto when Caddy holds a newer certificate. Run it daily from cron.
# Gateways reconnect on their own and their persistent sessions are kept.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

domain=$(require_value MQTT_DOMAIN)

caddy_cert=$(compose exec -T caddy sh -c \
	"find /data/caddy/certificates -type f -path '*/${domain}/${domain}.crt' | head -n 1")
[ -n "$caddy_cert" ] || die "Caddy has no certificate for $domain yet."

caddy_hash=$(compose exec -T caddy cat "$caddy_cert" | sha256sum)
broker_hash=$(compose exec -T mosquitto cat /mosquitto/certs/server.crt 2>/dev/null | sha256sum)

if [ "$caddy_hash" = "$broker_hash" ]; then
	echo "$(date -u +%FT%TZ) Mosquitto already uses the current certificate of $domain."
else
	compose restart mosquitto
	echo "$(date -u +%FT%TZ) Restarted Mosquitto with the renewed certificate of $domain."
fi
