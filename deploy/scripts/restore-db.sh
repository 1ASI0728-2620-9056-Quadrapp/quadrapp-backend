#!/usr/bin/env bash
# Restores a dump made by backup-db.sh. It replaces the current data of the database.
# Usage: scripts/restore-db.sh s3://<bucket>/<prefix>/<file>.dump [--yes]
# The backend is stopped during the restore, which runs in one transaction: if it fails,
# the database is left as it was.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

[ $# -ge 1 ] || die "Usage: $0 s3://<bucket>/<prefix>/<file>.dump [--yes]"
source_uri=$1
db=$(env_value POSTGRES_DB)
user=$(env_value POSTGRES_USER)
db=${db:-quadrapp}
user=${user:-quadrapp}

if [ "${2:-}" != "--yes" ]; then
	read -r -p "This replaces the data of database '$db' with $source_uri. Type the database name to continue: " answer
	[ "$answer" = "$db" ] || die "Cancelled."
fi

command -v aws >/dev/null || die "The AWS CLI is not installed."
work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT
file="$work_dir/restore.dump"
aws s3 cp "$source_uri" "$file" --only-show-errors

compose stop backend
compose exec -T postgres pg_restore -U "$user" -d "$db" --clean --if-exists --no-owner --single-transaction <"$file"
compose start backend
echo "$(date -u +%FT%TZ) Restored $source_uri into $db and started the backend."
