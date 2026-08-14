#!/usr/bin/env sh
set -eu

if [ "$#" -ne 2 ] || [ "$2" != "--confirm" ]; then
  echo "Usage: $0 <backup-directory> --confirm" >&2
  exit 1
fi

backup_dir=$1
infra_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
compose_file="$infra_dir/docker-compose.prod.yml"
env_file="${ENV_FILE:-$infra_dir/.env.prod}"

for required in trade_ai.sql.gz nacos_config.sql.gz SHA256SUMS; do
  [ -f "$backup_dir/$required" ] || { echo "Missing $backup_dir/$required" >&2; exit 1; }
done
(cd "$backup_dir" && sha256sum -c SHA256SUMS)

compose() {
  docker compose --env-file "$env_file" -f "$compose_file" "$@"
}

read_env() {
  sed -n "s/^$1=//p" "$env_file" | tail -n 1
}

compose stop gateway-service auth-service tenant-service customer-service product-service inquiry-service ai-service quotation-service order-service task-service notification-service file-service aigc-service
gzip -dc "$backup_dir/trade_ai.sql.gz" | compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot trade_ai'
gzip -dc "$backup_dir/nacos_config.sql.gz" | compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot nacos_config'

if [ -d "$backup_dir/minio" ]; then
  project_name=$(read_env COMPOSE_PROJECT_NAME)
  project_name=${project_name:-nexaflow}
  bucket=$(read_env MINIO_BUCKET)
  bucket=${bucket:-trade-ai}
  docker run --rm \
    --network "${project_name}_nexaflow-network" \
    -e "MC_HOST_storage=http://$(read_env MINIO_ROOT_USER):$(read_env MINIO_ROOT_PASSWORD)@minio:9000" \
    -v "$backup_dir/minio:/backup:ro" \
    minio/mc:RELEASE.2025-04-16T18-13-26Z mirror --overwrite /backup "storage/$bucket"
fi

compose up -d
echo "Restore completed. Run the readiness and smoke tests before reopening traffic."
