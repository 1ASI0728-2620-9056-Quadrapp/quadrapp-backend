#!/bin/sh
# Entrypoint of the mosquitto service: copies the TLS certificate that Caddy obtained for
# MQTT_DOMAIN (Caddy's data volume is mounted read-only at /caddy-data), then starts Mosquitto.
# If Caddy has no certificate yet, it exits and Docker restarts it until one exists.
set -eu

: "${MQTT_DOMAIN:?MQTT_DOMAIN is not set}"

crt=$(find /caddy-data/caddy/certificates -type f -path "*/${MQTT_DOMAIN}/${MQTT_DOMAIN}.crt" 2>/dev/null | head -n 1)
if [ -z "$crt" ]; then
	echo "No certificate for ${MQTT_DOMAIN} in Caddy's storage yet. Retrying in 15 seconds." >&2
	sleep 15
	exit 1
fi

mkdir -p /mosquitto/certs
cp "$crt" /mosquitto/certs/server.crt
cp "${crt%.crt}.key" /mosquitto/certs/server.key
chown -R 1883:1883 /mosquitto/certs
chmod 600 /mosquitto/certs/server.key
echo "Using the certificate ${crt}."

exec /docker-entrypoint.sh /usr/sbin/mosquitto -c /mosquitto/config/mosquitto.conf
