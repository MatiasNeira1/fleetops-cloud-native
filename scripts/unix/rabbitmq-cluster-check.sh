#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
COMPOSE_FILE="$REPO_ROOT/platform/local/docker-compose.yml"
ENV_FILE="$REPO_ROOT/.env"
RABBIT_API_BASE_URL="http://127.0.0.1:15672/api"

if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: falta .env. Copia .env.example a .env y define las variables RabbitMQ locales." >&2
  exit 1
fi

set -a
. "$ENV_FILE"
set +a

: "${RABBITMQ_DEFAULT_USER:?Falta RABBITMQ_DEFAULT_USER en .env}"
: "${RABBITMQ_DEFAULT_PASS:?Falta RABBITMQ_DEFAULT_PASS en .env}"

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

wait_healthy() {
  container="$1"
  deadline=$((SECONDS + 180))
  while [ "$SECONDS" -lt "$deadline" ]; do
    status=$(docker inspect --format '{{.State.Health.Status}}' "$container" 2>/dev/null || true)
    if [ "$status" = "healthy" ]; then
      echo "$container healthy"
      return 0
    fi
    sleep 3
  done
  echo "ERROR: $container no quedo healthy" >&2
  exit 1
}

cluster_status() {
  compose exec -T "$1" rabbitmqctl cluster_status
}

status_has_both_nodes() {
  status="$1"
  printf '%s' "$status" | grep -F "rabbit@rabbitmq-1" >/dev/null &&
    printf '%s' "$status" | grep -F "rabbit@rabbitmq-2" >/dev/null
}

repair_cluster_if_needed() {
  status1=$(cluster_status rabbitmq-1)
  status2=$(cluster_status rabbitmq-2)

  if status_has_both_nodes "$status1" && status_has_both_nodes "$status2"; then
    return 0
  fi

  echo "rabbitmq-2 no esta unido a rabbitmq-1. Ejecutando union idempotente del cluster..."
  compose exec -T rabbitmq-2 rabbitmqctl stop_app >/dev/null
  compose exec -T rabbitmq-2 rabbitmqctl reset >/dev/null
  compose exec -T rabbitmq-2 rabbitmqctl join_cluster rabbit@rabbitmq-1 >/dev/null
  compose exec -T rabbitmq-2 rabbitmqctl start_app >/dev/null
}

api_get() {
  curl -fsS -u "$RABBITMQ_DEFAULT_USER:$RABBITMQ_DEFAULT_PASS" "$RABBIT_API_BASE_URL$1"
}

wait_api() {
  deadline=$((SECONDS + 60))
  while [ "$SECONDS" -lt "$deadline" ]; do
    if api_get /overview >/dev/null 2>&1; then
      echo "RabbitMQ Management API disponible en $RABBIT_API_BASE_URL"
      return 0
    fi
    sleep 2
  done

  echo "ERROR: RabbitMQ Management API no respondio en $RABBIT_API_BASE_URL despues de 60s." >&2
  exit 1
}

require_text() {
  text="$1"
  expected="$2"
  label="$3"
  printf '%s' "$text" | grep -F "$expected" >/dev/null || {
    echo "ERROR: no se encontro $expected en $label" >&2
    exit 1
  }
}

cd "$REPO_ROOT"
wait_healthy fleetops-rabbitmq-1
wait_healthy fleetops-rabbitmq-2
wait_api

repair_cluster_if_needed

status1=$(cluster_status rabbitmq-1)
status2=$(cluster_status rabbitmq-2)

require_text "$status1" "rabbit@rabbitmq-1" "cluster_status rabbitmq-1"
require_text "$status1" "rabbit@rabbitmq-2" "cluster_status rabbitmq-1"
require_text "$status2" "rabbit@rabbitmq-1" "cluster_status rabbitmq-2"
require_text "$status2" "rabbit@rabbitmq-2" "cluster_status rabbitmq-2"
echo "Cluster OK: rabbit@rabbitmq-1 y rabbit@rabbitmq-2 visibles desde ambos nodos"

exchanges=$(api_get /exchanges/%2F)
queues=$(api_get /queues/%2F)
bindings=$(api_get /bindings/%2F)

for item in \
  '"name":"fleetops.logs.direct"' '"type":"direct"' \
  '"name":"fleetops.events.topic"' '"type":"topic"' \
  '"name":"fleetops.dlx.direct"' \
  '"name":"fleetops.logs.all.queue"' \
  '"name":"fleetops.logs.errors.queue"' \
  '"name":"fleetops.logs.security.queue"' \
  '"name":"fleetops.logs.all.dlq"' \
  '"name":"fleetops.logs.errors.dlq"' \
  '"name":"fleetops.logs.security.dlq"'
do
  require_text "$exchanges$queues" "$item" "topologia"
done

for item in \
  '"x-dead-letter-exchange":"fleetops.dlx.direct"' \
  '"x-dead-letter-routing-key":"fleetops.logs.all.dead"' \
  '"x-dead-letter-routing-key":"fleetops.logs.errors.dead"' \
  '"x-dead-letter-routing-key":"fleetops.logs.security.dead"' \
  '"routing_key":"INFO"' '"routing_key":"WARNING"' '"routing_key":"ERROR"' '"routing_key":"SECURITY"' \
  '"routing_key":"fleetops.#"' '"routing_key":"fleetops.*.error"' '"routing_key":"fleetops.security.#"' \
  '"routing_key":"fleetops.logs.all.dead"' '"routing_key":"fleetops.logs.errors.dead"' '"routing_key":"fleetops.logs.security.dead"'
do
  require_text "$queues$bindings" "$item" "argumentos/bindings"
done

echo "RabbitMQ cluster y topologia OK"
