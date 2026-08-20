#!/usr/bin/env sh
set -eu

infra_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
env_file="${ENV_FILE:-$infra_dir/.env.prod}"
compose_file="$infra_dir/docker-compose.prod.yml"

[ -f "$env_file" ] || { echo "Missing environment file: $env_file" >&2; exit 1; }
docker compose version >/dev/null 2>&1 || { echo "Docker Compose v2 is required." >&2; exit 1; }

echo "This imports only the NexaFlow demo tenant data into trade_ai."
docker compose --env-file "$env_file" -f "$compose_file" exec -T mysql \
  sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot trade_ai' \
  < "$infra_dir/mysql/demo/001_business_demo_data.sql"
echo "Demo data loaded. Login: demo_owner / admin123"
