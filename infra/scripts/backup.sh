#!/usr/bin/env sh
set -eu

infra_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
compose_file="$infra_dir/docker-compose.prod.yml"
env_file="${ENV_FILE:-$infra_dir/.env.prod}"

if [ ! -f "$env_file" ]; then
  echo "Missing environment file: $env_file" >&2
  exit 1
fi

compose() {
  docker compose --env-file "$env_file" -f "$compose_file" "$@"
}

read_env() {
  sed -n "s/^$1=//p" "$env_file" | tail -n 1
}

backup_root=$(read_env BACKUP_DIR)
backup_root=${backup_root:-/opt/nexaflow/backups}
retention_days=$(read_env BACKUP_RETENTION_DAYS)
retention_days=${retention_days:-14}
case "$retention_days" in
  ''|*[!0-9]*) echo "BACKUP_RETENTION_DAYS must be a positive integer." >&2; exit 1 ;;
esac
case "$backup_root" in
  /|/opt|/opt/nexaflow) echo "BACKUP_DIR must be a dedicated backup directory." >&2; exit 1 ;;
esac
bucket=$(read_env MINIO_BUCKET)
bucket=${bucket:-trade-ai}
stamp=$(date -u +%Y%m%dT%H%M%SZ)
target="$backup_root/$stamp"
mkdir -p "$target/minio"

compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --events --triggers --set-gtid-purged=OFF trade_ai' | gzip -9 > "$target/trade_ai.sql.gz"
compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --events --triggers --set-gtid-purged=OFF nacos_config' | gzip -9 > "$target/nacos_config.sql.gz"

project_name=$(read_env COMPOSE_PROJECT_NAME)
project_name=${project_name:-nexaflow}
docker run --rm \
  --network "${project_name}_nexaflow-network" \
  -e "MC_HOST_storage=http://$(read_env MINIO_ROOT_USER):$(read_env MINIO_ROOT_PASSWORD)@minio:9000" \
  -v "$target/minio:/backup" \
  minio/mc:RELEASE.2025-04-16T18-13-26Z mirror --overwrite "storage/$bucket" /backup

(cd "$target" && sha256sum trade_ai.sql.gz nacos_config.sql.gz > SHA256SUMS)
find "$backup_root" -mindepth 1 -maxdepth 1 -type d -mtime "+$retention_days" -exec rm -rf -- {} +
echo "Backup completed: $target"
