#!/usr/bin/env bash
set -euo pipefail

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
env_file="$repo_root/.env"
[[ -f "$env_file" ]] || { echo "Falta .env en la raíz" >&2; exit 1; }
set -a
source "$env_file"
set +a
: "${RABBITMQ_DEFAULT_USER:?Falta RABBITMQ_DEFAULT_USER}"
: "${RABBITMQ_DEFAULT_PASS:?Falta RABBITMQ_DEFAULT_PASS}"
command -v jq >/dev/null || { echo "Se requiere jq" >&2; exit 1; }

fleet_url="http://127.0.0.1:8080"
alerts_url="http://127.0.0.1:8081"
rabbit_url="http://127.0.0.1:${RABBITMQ_NODE1_MANAGEMENT_PORT:-15672}/api"
queue_name="${RABBITMQ_ALERTS_QUEUE:-fleetops.alerts.queue}"
dlq_name="${RABBITMQ_ALERTS_DLQ:-fleetops.alerts.dlq}"
exchange_name="${RABBITMQ_ALERTS_EXCHANGE:-fleetops.events.topic}"

wait_health() {
  local url="$1"
  local path="$2"
  local endpoint="$url/actuator/health"
  [[ -n "$path" ]] && endpoint="$endpoint/$path"
  for ((attempt=0; attempt<60; attempt++)); do
    if curl -fsS "$endpoint" >/dev/null 2>&1; then return; fi
    sleep 2
  done
  echo "Servicio no saludable: $url" >&2
  exit 1
}

rabbit() {
  curl -fsS --user "$RABBITMQ_DEFAULT_USER:$RABBITMQ_DEFAULT_PASS" "$rabbit_url$1"
}

queue_stat() {
  rabbit "/queues/%2F/$1" | jq -r ".${2} // 0"
}

wait_health "$fleet_url" ""
wait_health "$alerts_url" "readiness"
rabbit /overview >/dev/null
rabbit /nodes | jq -e 'length >= 2' >/dev/null || { echo "Clúster RabbitMQ incompleto" >&2; exit 1; }
rabbit "/queues/%2F/$queue_name" >/dev/null
rabbit "/queues/%2F/$dlq_name" >/dev/null

before_alerts=$(curl -fsS "$alerts_url/api/v1/trips/trip-001/alerts?size=1" | jq -r .totalElements)
before_ack=$(queue_stat "$queue_name" message_stats.ack)
for kind in speed fuel; do
  event_id=$(cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen)
  speed=50
  fuel=50
  [[ "$kind" == speed ]] && speed=105
  [[ "$kind" == fuel ]] && fuel=8
  payload=$(jq -nc --arg eventId "$event_id" --arg now "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    --argjson speed "$speed" --argjson fuel "$fuel" \
    '{eventId:$eventId,vehicleId:"vehicle-001",tripId:"trip-001",latitude:-33.2321,longitude:-71.0201,speedKph:$speed,fuelPercent:$fuel,odometerKm:128540.7,recordedAt:$now}')
  curl -fsS -H 'content-type: application/json' -d "$payload" "$fleet_url/api/v1/telemetry" | jq -e '.status == "ACCEPTED"' >/dev/null
  expected=$((before_alerts + 1))
  matched=false
  for ((attempt=0; attempt<60; attempt++)); do
    alerts=$(curl -fsS "$alerts_url/api/v1/trips/trip-001/alerts?size=100")
    current=$(printf '%s' "$alerts" | jq -r .totalElements)
    if [[ "$current" -ge "$expected" ]] && printf '%s' "$alerts" | jq -e --arg id "$event_id" 'any(.content[]; .correlationId == $id)' >/dev/null; then
      matched=true
      break
    fi
    sleep 2
  done
  [[ "$matched" == true ]] || { echo "No apareció alerta nueva de $kind con correlationId $event_id" >&2; exit 1; }
  before_alerts="$current"
  echo "Alerta $kind persistida; total del viaje: $current"
done

after_ack=$(queue_stat "$queue_name" message_stats.ack)
[[ "$after_ack" -ge $((before_ack + 2)) ]] || { echo "No aumentó ack de $queue_name" >&2; exit 1; }
before_dlq=$(queue_stat "$dlq_name" messages)
invalid=$(jq -nc '{eventId:"invalid-controlled-demo",eventType:"alert.created",eventVersion:999}')
publish=$(jq -nc --arg payload "$invalid" '{properties:{content_type:"application/json"},routing_key:"fleetops.alert.speed.created",payload:$payload,payload_encoding:"string"}')
curl -fsS --user "$RABBITMQ_DEFAULT_USER:$RABBITMQ_DEFAULT_PASS" -H 'content-type: application/json' \
  -d "$publish" "$rabbit_url/exchanges/%2F/$exchange_name/publish" | jq -e '.routed == true' >/dev/null
for ((attempt=0; attempt<30; attempt++)); do
  after_dlq=$(queue_stat "$dlq_name" messages)
  [[ "$after_dlq" -gt "$before_dlq" ]] && break
  sleep 1
done
[[ "$after_dlq" -gt "$before_dlq" ]] || { echo "No aumentó $dlq_name" >&2; exit 1; }
echo "ACK $before_ack -> $after_ack; DLQ $before_dlq -> $after_dlq"
