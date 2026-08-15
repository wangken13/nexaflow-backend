#!/usr/bin/env sh
set -eu

infra_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
backend_dir=$(CDPATH= cd -- "$infra_dir/.." && pwd)
frontend_dir=$(CDPATH= cd -- "$backend_dir/../tradeflow-ai-frontend" && pwd)
compose_file="$infra_dir/docker-compose.prod.yml"
env_file="${ENV_FILE:-$infra_dir/.env.prod}"
maven_image="${MAVEN_IMAGE:-maven:3.9.11-eclipse-temurin-21}"
maven_settings="$infra_dir/maven-settings.xml"
maven_cache_volume="${MAVEN_CACHE_VOLUME:-nexaflow-maven-cache}"
release_tag="${IMAGE_TAG:-$(git -C "$backend_dir" rev-parse --short HEAD 2>/dev/null || date +%Y%m%d%H%M%S)}"
export IMAGE_TAG="$release_tag"

infrastructure_services="mysql redis rabbitmq minio"
business_services="auth-service tenant-service customer-service product-service inquiry-service ai-service quotation-service order-service task-service notification-service file-service aigc-service"
post_migration_services="tenant-service customer-service product-service inquiry-service ai-service quotation-service order-service task-service notification-service file-service aigc-service"
boot_modules="gateway-service auth-service/biz tenant-service/biz customer-service/biz product-service/biz inquiry-service/biz ai-service/biz quotation-service/biz order-service/biz task-service/biz notification-service/biz file-service/biz aigc-service"

die() {
  echo "ERROR: $*" >&2
  exit 1
}

command -v docker >/dev/null 2>&1 || die "Docker is required."
docker compose version >/dev/null 2>&1 || die "Docker Compose v2 is required."
[ -f "$env_file" ] || die "Missing environment file: $env_file"
[ -d "$frontend_dir" ] || die "Missing frontend repository: $frontend_dir"
[ -f "$maven_settings" ] || die "Missing Maven settings: $maven_settings"

compose() {
  docker compose --env-file "$env_file" -f "$compose_file" "$@"
}

read_env() {
  sed -n "s/^$1=//p" "$env_file" | tail -n 1 | tr -d '\r'
}

show_service_diagnostics() {
  service_name=$1
  compose ps "$service_name" || true
  compose logs --tail=160 "$service_name" || true
}

wait_healthy() {
  service_name=$1
  max_attempts=${2:-180}
  attempt=0
  while [ "$attempt" -lt "$max_attempts" ]; do
    container_id=$(compose ps -q "$service_name")
    if [ -n "$container_id" ]; then
      status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id" 2>/dev/null || true)
      container_state=$(docker inspect --format '{{.State.Status}}' "$container_id" 2>/dev/null || true)
      if [ "$status" = "healthy" ]; then
        echo "[ready] $service_name"
        return 0
      fi
      if [ "$container_state" = "exited" ] || [ "$container_state" = "dead" ] \
          || { [ "$container_state" = "restarting" ] && [ "$attempt" -ge 3 ]; }; then
        show_service_diagnostics "$service_name"
        die "$service_name entered an unrecoverable state during startup: $container_state"
      fi
    fi
    attempt=$((attempt + 1))
    if [ $((attempt % 12)) -eq 0 ]; then
      echo "[wait] $service_name ($((attempt * 5))s)"
    fi
    sleep 5
  done
  show_service_diagnostics "$service_name"
  die "$service_name did not become healthy within $((max_attempts * 5)) seconds."
}

validate_nacos_credentials() {
  nacos_username=$(read_env NACOS_USERNAME)
  nacos_password=$(read_env NACOS_PASSWORD)
  case "$nacos_username" in
    ''|*[!A-Za-z0-9_.-]*) die "NACOS_USERNAME may only contain letters, digits, dots, underscores and hyphens." ;;
  esac
  if [ "${#nacos_password}" -lt 12 ] || [ "$nacos_password" = "replace-with-a-strong-nacos-password" ]; then
    die "NACOS_PASSWORD must be a real password of at least 12 characters."
  fi
}

initialize_rabbitmq_user() {
  rabbitmq_username=$(read_env RABBITMQ_USER)
  rabbitmq_password=$(read_env RABBITMQ_PASSWORD)
  case "$rabbitmq_username" in
    ''|*[!A-Za-z0-9_.-]*) die "RABBITMQ_USER may only contain letters, digits, dots, underscores and hyphens." ;;
  esac
  [ "${#rabbitmq_password}" -ge 12 ] || die "RABBITMQ_PASSWORD must contain at least 12 characters."

  if compose exec -T rabbitmq rabbitmqctl list_users -q | awk '{print $1}' | grep -Fxq "$rabbitmq_username"; then
    compose exec -T rabbitmq rabbitmqctl change_password "$rabbitmq_username" "$rabbitmq_password"
  else
    compose exec -T rabbitmq rabbitmqctl add_user "$rabbitmq_username" "$rabbitmq_password"
  fi
  compose exec -T rabbitmq rabbitmqctl set_permissions -p / "$rabbitmq_username" '.*' '.*' '.*'
}

initialize_nacos_admin() {
  nacos_username=$(read_env NACOS_USERNAME)
  nacos_password=$(read_env NACOS_PASSWORD)
  nacos_hash=$(docker run --rm httpd:2.4-alpine htpasswd -bnBC 10 "" "$nacos_password" | cut -d: -f2)
  [ -n "$nacos_hash" ] || die "Failed to generate the Nacos password hash."
  printf '%s\n' \
    "INSERT INTO users (username, password, enabled) VALUES ('$nacos_username', '$nacos_hash', TRUE) ON DUPLICATE KEY UPDATE password = VALUES(password), enabled = TRUE;" \
    "INSERT INTO roles (username, role) VALUES ('$nacos_username', 'ROLE_ADMIN') ON DUPLICATE KEY UPDATE role = VALUES(role);" \
    | compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot nacos_config'
}

repair_failed_migration() {
  migration_version=$1
  repair_file=$2
  failed_count=$(compose exec -T mysql sh -c \
    "MYSQL_PWD=\"\$MYSQL_PASSWORD\" mysql -N -B -u\"\$MYSQL_USER\" trade_ai -e \"SELECT COUNT(*) FROM flyway_schema_history WHERE version = '$migration_version' AND success = 0\"" \
    2>/dev/null || true)
  if [ -n "$failed_count" ] && [ "$failed_count" != "0" ]; then
    echo "[repair] complete failed Flyway migration V$migration_version"
    compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" trade_ai' < "$repair_file"
  fi
}

repair_known_failed_migrations() {
  repair_failed_migration "018" "$infra_dir/mysql/repair/V018__complete_operation_audit_repair.sql"
  repair_failed_migration "019" "$infra_dir/mysql/repair/V019__import_jobs_repair.sql"
}

package_backend() {
  if [ "${SKIP_PACKAGE:-false}" = "true" ]; then
    echo "[skip] backend package"
    return 0
  fi

  if [ "${RUN_TESTS:-false}" = "true" ]; then
    maven_goal="clean verify"
    maven_flags="-DskipTests=false"
  else
    maven_goal="clean package"
    maven_flags="-Dmaven.test.skip=true"
  fi

  docker run --rm \
    -v "$maven_cache_volume:/root/.m2" \
    -v "$maven_settings:/tmp/maven-settings.xml:ro" \
    -v "$backend_dir:/workspace" \
    -w /workspace \
    "$maven_image" \
    mvn -s /tmp/maven-settings.xml -B $maven_flags $maven_goal
}

validate_boot_jars() {
  modules="$boot_modules" docker run --rm \
    -e modules \
    -v "$backend_dir:/workspace:ro" \
    -w /workspace \
    "$maven_image" \
    sh -ec '
      for module in $modules; do
        set -- "$module"/target/*.jar
        [ "$#" -eq 1 ] && [ -f "$1" ] || { echo "Missing executable JAR for $module" >&2; exit 1; }
        jar tf "$1" | grep -q "^BOOT-INF/" || { echo "Non-executable Spring Boot JAR: $1" >&2; exit 1; }
        echo "[jar] $1"
      done
    '
}

verify_routes() {
  compose exec -T frontend wget -q -O /dev/null --timeout=15 http://gateway-service:18080/readyz \
    || die "Frontend cannot reach the gateway readiness endpoint."
  compose exec -T frontend wget -q -O /dev/null --timeout=30 http://gateway-service:18080/api/auth/captcha \
    || die "Gateway cannot route requests to auth-service."

  http_port=$(read_env HTTP_PORT)
  http_port=${http_port:-80}
  curl -fsS --max-time 15 "http://127.0.0.1:$http_port/readyz" >/dev/null \
    || die "The public readiness endpoint is unavailable."
}

echo "[1/9] Validate production configuration"
validate_nacos_credentials
compose config --quiet

echo "[2/9] Start infrastructure"
compose up -d $infrastructure_services
for service in $infrastructure_services; do
  wait_healthy "$service" 120
done
initialize_rabbitmq_user

echo "[3/9] Initialize and start Nacos"
initialize_nacos_admin
repair_known_failed_migrations
compose up -d nacos
wait_healthy nacos 180

echo "[4/9] Package backend"
package_backend

echo "[5/9] Validate Spring Boot artifacts"
validate_boot_jars

echo "[6/9] Build release images: $release_tag"
compose build

echo "[7/9] Start gateway"
compose up -d --force-recreate --no-deps gateway-service
wait_healthy gateway-service 180

echo "[8/9] Start business services"
echo "[migrate] start auth-service as the exclusive Flyway migration owner"
compose up -d --force-recreate --no-deps auth-service
wait_healthy auth-service 180
compose up -d --force-recreate --no-deps $post_migration_services
for service in $post_migration_services; do
  wait_healthy "$service" 180
done

echo "[9/9] Start frontend and verify end-to-end routing"
compose up -d --force-recreate --no-deps frontend
wait_healthy frontend 60
verify_routes

compose ps
echo "Release image tag: $release_tag"
echo "Deployment completed successfully."
