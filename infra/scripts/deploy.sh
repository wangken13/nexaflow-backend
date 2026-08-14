#!/usr/bin/env sh
set -eu

infra_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
backend_dir=$(CDPATH= cd -- "$infra_dir/.." && pwd)
frontend_dir=$(CDPATH= cd -- "$backend_dir/../tradeflow-ai-frontend" && pwd)
compose_file="$infra_dir/docker-compose.prod.yml"
env_file="${ENV_FILE:-$infra_dir/.env.prod}"
release_tag="${IMAGE_TAG:-$(git -C "$backend_dir" rev-parse --short HEAD 2>/dev/null || date +%Y%m%d%H%M%S)}"
export IMAGE_TAG="$release_tag"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required." >&2
  exit 1
fi

if [ ! -f "$env_file" ]; then
  echo "Missing environment file: $env_file" >&2
  echo "Create it from $infra_dir/.env.prod.example without committing it." >&2
  exit 1
fi

if [ ! -d "$frontend_dir" ]; then
  echo "Missing frontend repository: $frontend_dir" >&2
  exit 1
fi

compose() {
  docker compose --env-file "$env_file" -f "$compose_file" "$@"
}

read_env() {
  sed -n "s/^$1=//p" "$env_file" | tail -n 1
}

wait_healthy() {
  service_name=$1
  attempt=0
  while [ "$attempt" -lt 60 ]; do
    container_id=$(compose ps -q "$service_name")
    if [ -n "$container_id" ] && [ "$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{end}}' "$container_id")" = "healthy" ]; then
      return 0
    fi
    attempt=$((attempt + 1))
    sleep 2
  done
  echo "$service_name did not become healthy. Check: docker compose -f $compose_file logs $service_name" >&2
  exit 1
}

nacos_username=$(read_env NACOS_USERNAME)
nacos_password=$(read_env NACOS_PASSWORD)
case "$nacos_username" in
  ''|*[!A-Za-z0-9_.-]*)
    echo "NACOS_USERNAME may only contain letters, digits, dots, underscores and hyphens." >&2
    exit 1
    ;;
esac

if [ "${#nacos_password}" -lt 12 ] || [ "$nacos_password" = "replace-with-a-strong-nacos-password" ]; then
  echo "NACOS_PASSWORD must be a real password of at least 12 characters." >&2
  exit 1
fi

compose up -d mysql redis rabbitmq minio
wait_healthy mysql

nacos_hash=$(docker run --rm httpd:2.4-alpine htpasswd -bnBC 10 "" "$nacos_password" | cut -d: -f2)
printf '%s\n' "INSERT INTO users (username, password, enabled) VALUES ('$nacos_username', '$nacos_hash', TRUE) ON DUPLICATE KEY UPDATE password = VALUES(password), enabled = TRUE;" "INSERT INTO roles (username, role) VALUES ('$nacos_username', 'ROLE_ADMIN') ON DUPLICATE KEY UPDATE role = VALUES(role);" | compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot nacos_config'

compose up -d nacos
wait_healthy nacos

if [ "${SKIP_PACKAGE:-false}" != "true" ]; then
  docker run --rm -v "$backend_dir:/workspace" -w /workspace maven:3.9.11-eclipse-temurin-21 mvn -B -DskipTests package
fi

compose up -d --build --remove-orphans
wait_healthy frontend

http_port=$(read_env HTTP_PORT)
http_port=${http_port:-80}
attempt=0
while [ "$attempt" -lt 30 ]; do
  if curl -fsS "http://127.0.0.1:$http_port/readyz" >/dev/null 2>&1; then
    break
  fi
  attempt=$((attempt + 1))
  sleep 2
done
if [ "$attempt" -eq 30 ]; then
  echo "The public entry is running, but the gateway readiness probe failed." >&2
  compose logs --tail=120 gateway-service
  exit 1
fi

compose ps
echo "Release image tag: $release_tag"
echo "Deployment completed. Readiness probe: http://127.0.0.1:$http_port/readyz"
