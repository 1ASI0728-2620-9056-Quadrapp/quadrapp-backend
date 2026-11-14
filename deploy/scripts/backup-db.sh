#!/usr/bin/env bash
# Dumps the PostgreSQL database and uploads it to S3 (decision D7b: daily pg_dump to S3).
# Runs on the instance, where the AWS CLI uses the instance role (s3:PutObject on the bucket).
# Usage: scripts/backup-db.sh   (cron: see deploy/README.md)
# The dump is checked with pg_restore --list before the upload, so a failed dump is never
# uploaded. Old dumps are removed by an S3 lifecycle rule, not by this script.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

command -v aws >/dev/null || die "The AWS CLI is not installed."
target=$(require_value BACKUP_S3_URI)
db=$(env_value POSTGRES_DB)
user=$(env_value POSTGRES_USER)
db=${db:-quadrapp}
user=${user:-quadrapp}

work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT
file="$work_dir/${db}-$(date -u +%Y%m%dT%H%M%SZ).dump"

compose exec -T postgres pg_dump -U "$user" -d "$db" --format=custom >"$file"
compose exec -T postgres pg_restore --list >/dev/null <"$file"

aws s3 cp "$file" "${target%/}/$(basename "$file")" --only-show-errors
echo "$(date -u +%FT%TZ) Uploaded ${target%/}/$(basename "$file") ($(du -h "$file" | cut -f 1))."
