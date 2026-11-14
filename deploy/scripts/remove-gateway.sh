#!/usr/bin/env bash
# Removes the MQTT credential of one gateway; its open connection is closed by the broker.
# Usage: scripts/remove-gateway.sh <tenantId> <gatewayId>
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

[ $# -eq 2 ] || die "Usage: $0 <tenantId> <gatewayId>"
require_id tenantId "$1"
require_id gatewayId "$2"
name="gw-$1-$2"

dynsec deleteClient "$name"
dynsec deleteRole "$name"
echo "Removed the client and the role $name."
