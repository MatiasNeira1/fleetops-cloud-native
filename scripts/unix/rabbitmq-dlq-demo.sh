#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
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

api() {
  method="$1"
  path="$2"
  body="${3:-}"
  if [ -n "$body" ]; then
    curl -fsS -u "$RABBITMQ_DEFAULT_USER:$RABBITMQ_DEFAULT_PASS" \
      -H 'content-type: application/json' \
      -X "$method" \
      -d "$body" \
      "$RABBIT_API_BASE_URL$path"
  else
    curl -fsS -u "$RABBITMQ_DEFAULT_USER:$RABBITMQ_DEFAULT_PASS" \
      -X "$method" \
      "$RABBIT_API_BASE_URL$path"
  fi
}

wait_api() {
  deadline=$((SECONDS + 60))
  while [ "$SECONDS" -lt "$deadline" ]; do
    if api GET /overview >/dev/null 2>&1; then
      echo "RabbitMQ Management API disponible en $RABBIT_API_BASE_URL"
      return 0
    fi
    sleep 2
  done

  echo "ERROR: RabbitMQ Management API no respondio en $RABBIT_API_BASE_URL despues de 60s." >&2
  exit 1
}

purge_queue() {
  api DELETE "/queues/%2F/$1/contents" >/dev/null
}

publish_message() {
  exchange="$1"
  routing_key="$2"
  payload="$3"
  response=$(api POST "/exchanges/%2F/$exchange/publish" \
    "{\"properties\":{\"content_type\":\"application/json\"},\"routing_key\":\"$routing_key\",\"payload\":\"\\\"$payload\\\"\",\"payload_encoding\":\"string\"}")
  printf '%s' "$response" | grep '"routed":true' >/dev/null || {
    echo "ERROR: mensaje no ruteado por $exchange con $routing_key" >&2
    exit 1
  }
}

queue_messages() {
  api GET "/queues/%2F/$1" | sed -n 's/.*"messages":\([0-9][0-9]*\).*/\1/p' | head -n 1
}

wait_queue_messages() {
  queue="$1"
  expected="$2"
  deadline=$((SECONDS + 30))
  while [ "$SECONDS" -lt "$deadline" ]; do
    count=$(queue_messages "$queue")
    count=${count:-0}
    if [ "$count" -ge "$expected" ]; then
      echo "$queue tiene $count mensaje(s)"
      return 0
    fi
    sleep 1
  done
  echo "ERROR: $queue no alcanzo $expected mensaje(s)" >&2
  exit 1
}

get_one_message() {
  queue="$1"
  ackmode="$2"
  response=$(api POST "/queues/%2F/$queue/get" \
    "{\"count\":1,\"ackmode\":\"$ackmode\",\"encoding\":\"auto\",\"truncate\":50000}")
  [ "$response" != "[]" ] || {
    echo "ERROR: no se pudo obtener mensaje desde $queue" >&2
    exit 1
  }
}

run_dlq_case() {
  name="$1"
  exchange="$2"
  routing_key="$3"
  main_queue="$4"
  dlq="$5"

  echo "Caso DLQ: $name"
  purge_queue "$main_queue"
  initial_dlq_messages=$(queue_messages "$dlq")
  initial_dlq_messages=${initial_dlq_messages:-0}

  publish_message "$exchange" "$routing_key" "fleetops demo valido $name"
  wait_queue_messages "$main_queue" 1
  get_one_message "$main_queue" ack_requeue_false

  publish_message "$exchange" "$routing_key" "fleetops demo rechazo controlado $name"
  wait_queue_messages "$main_queue" 1
  get_one_message "$main_queue" reject_requeue_false
  wait_queue_messages "$dlq" $((initial_dlq_messages + 1))
}

wait_api

run_dlq_case general fleetops.logs.direct INFO fleetops.logs.all.queue fleetops.logs.all.dlq
run_dlq_case errores fleetops.events.topic fleetops.vehicle.error fleetops.logs.errors.queue fleetops.logs.errors.dlq
run_dlq_case seguridad fleetops.events.topic fleetops.security.audit fleetops.logs.security.queue fleetops.logs.security.dlq

purge_queue fleetops.logs.all.queue
purge_queue fleetops.logs.errors.queue
purge_queue fleetops.logs.security.queue

echo "Demo DLQ OK: las tres DLQ recibieron mensajes"
