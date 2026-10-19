#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE="$REPO_ROOT/.env"
SERVICE_DIR="$REPO_ROOT/backend/servicios/servicio-flota"
MAVEN_WRAPPER="$SERVICE_DIR/mvnw"

if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: falta .env en la raiz del repositorio." >&2
  exit 1
fi

if [ ! -x "$MAVEN_WRAPPER" ]; then
  echo "ERROR: no se encontro Maven Wrapper ejecutable en $MAVEN_WRAPPER" >&2
  exit 1
fi

read_env_value() {
  name="$1"
  value=$(grep -E "^[[:space:]]*$name[[:space:]]*=" "$ENV_FILE" | tail -n 1 | cut -d= -f2- || true)
  printf '%s' "$value" | sed "s/^[[:space:]]*//;s/[[:space:]]*$//;s/^['\"]//;s/['\"]$//"
}

rabbit_user=$(read_env_value SPRING_RABBITMQ_USERNAME)
if [ -z "$rabbit_user" ]; then
  rabbit_user=$(read_env_value RABBITMQ_DEFAULT_USER)
fi

rabbit_pass=$(read_env_value SPRING_RABBITMQ_PASSWORD)
if [ -z "$rabbit_pass" ]; then
  rabbit_pass=$(read_env_value RABBITMQ_DEFAULT_PASS)
fi

if [ -z "$rabbit_user" ]; then
  echo "ERROR: falta SPRING_RABBITMQ_USERNAME o RABBITMQ_DEFAULT_USER en .env" >&2
  exit 1
fi

if [ -z "$rabbit_pass" ]; then
  echo "ERROR: falta SPRING_RABBITMQ_PASSWORD o RABBITMQ_DEFAULT_PASS en .env" >&2
  exit 1
fi

postgres_user=$(read_env_value POSTGRES_USER)
postgres_password=$(read_env_value POSTGRES_PASSWORD)
if [ -z "$postgres_user" ]; then
  echo "ERROR: falta POSTGRES_USER en .env" >&2
  exit 1
fi
if [ -z "$postgres_password" ]; then
  echo "ERROR: falta POSTGRES_PASSWORD en .env" >&2
  exit 1
fi

postgres_host=$(read_env_value POSTGRES_HOST)
postgres_port=$(read_env_value POSTGRES_PORT)
postgres_db=$(read_env_value POSTGRES_DB)
demo_data_enabled=$(read_env_value FLEETOPS_DEMO_DATA_ENABLED)
telemetry_max_speed=$(read_env_value FLEETOPS_TELEMETRY_MAX_SPEED_KPH)
telemetry_min_fuel=$(read_env_value FLEETOPS_TELEMETRY_MIN_FUEL_PERCENT)

export SPRING_RABBITMQ_ADDRESSES="localhost:5672,localhost:5673"
export SPRING_RABBITMQ_USERNAME="$rabbit_user"
export SPRING_RABBITMQ_PASSWORD="$rabbit_pass"
export SPRING_PROFILES_ACTIVE="local"
[ -z "$postgres_host" ] || export POSTGRES_HOST="$postgres_host"
[ -z "$postgres_port" ] || export POSTGRES_PORT="$postgres_port"
[ -z "$postgres_db" ] || export POSTGRES_DB="$postgres_db"
[ -z "$postgres_user" ] || export POSTGRES_USER="$postgres_user"
[ -z "$postgres_password" ] || export POSTGRES_PASSWORD="$postgres_password"
[ -z "$demo_data_enabled" ] || export FLEETOPS_DEMO_DATA_ENABLED="$demo_data_enabled"
[ -z "$telemetry_max_speed" ] || export FLEETOPS_TELEMETRY_MAX_SPEED_KPH="$telemetry_max_speed"
[ -z "$telemetry_min_fuel" ] || export FLEETOPS_TELEMETRY_MIN_FUEL_PERCENT="$telemetry_min_fuel"

echo "Iniciando servicio-flota con RabbitMQ local publicado en localhost:5672,localhost:5673"

cd "$SERVICE_DIR"
exec "$MAVEN_WRAPPER" spring-boot:run
