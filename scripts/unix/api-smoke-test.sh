#!/usr/bin/env sh
set -eu

BASE_URL="${BASE_URL:-http://localhost:8080}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-60}"

request() {
  name="$1"
  method="$2"
  path="$3"
  expected="$4"
  body="${5:-}"
  tmp_body="$(mktemp)"

  if [ -n "$body" ]; then
    status=$(curl -sS -o "$tmp_body" -w "%{http_code}" -X "$method" \
      -H "Content-Type: application/json" \
      -d "$body" \
      "$BASE_URL$path")
  else
    status=$(curl -sS -o "$tmp_body" -w "%{http_code}" -X "$method" "$BASE_URL$path")
  fi

  if [ "$status" != "$expected" ]; then
    echo "ERROR: $name fallo con HTTP $status en $method $path" >&2
    cat "$tmp_body" >&2
    rm -f "$tmp_body"
    exit 1
  fi

  echo "OK $name -> HTTP $status" >&2
  cat "$tmp_body"
  rm -f "$tmp_body"
}

deadline=$(( $(date +%s) + TIMEOUT_SECONDS ))
while :; do
  if curl -fsS "$BASE_URL/actuator/health" >/dev/null 2>&1; then
    echo "OK health -> HTTP 200" >&2
    break
  fi
  if [ "$(date +%s)" -ge "$deadline" ]; then
    echo "ERROR: La API no respondio en $BASE_URL despues de $TIMEOUT_SECONDS segundos." >&2
    echo "Inicia el backend con ./scripts/unix/local-backend-run.sh" >&2
    exit 1
  fi
  sleep 2
done

trips="$(request "listar viajes" GET "/api/v1/trips" 200)"
case "$trips" in
  "[]"|"") echo "ERROR: No hay viajes demo. Define FLEETOPS_DEMO_DATA_ENABLED=true en .env y reinicia el backend con una base vacia." >&2; exit 1 ;;
esac

request "filtrar viajes IN_TRANSIT" GET "/api/v1/trips?status=IN_TRANSIT" 200 >/dev/null
request "detalle trip-001" GET "/api/v1/trips/trip-001" 200 >/dev/null
request "alertas trip-003" GET "/api/v1/trips/trip-003/alerts" 200 >/dev/null
request "posicion vehicle-001" GET "/api/v1/vehicles/vehicle-001/position" 200 >/dev/null

event_id="$(date +%s)-$$"
telemetry="{\"eventId\":\"$event_id\",\"vehicleId\":\"vehicle-001\",\"tripId\":\"trip-001\",\"latitude\":-33.2321,\"longitude\":-71.0201,\"speedKph\":95.5,\"fuelPercent\":10.2,\"odometerKm\":128540.7,\"recordedAt\":\"2026-10-11T13:15:30Z\"}"
request "enviar telemetria" POST "/api/v1/telemetry" 202 "$telemetry" >/dev/null

incident_body='{"tripId":"trip-001","vehicleId":"vehicle-001","type":"MECHANICAL_FAILURE","title":"Smoke test incidente","description":"Incidente generado por api-smoke-test.sh","severity":"CRITICAL","latitude":-33.2321,"longitude":-71.0201}'
incident="$(request "crear incidente" POST "/api/v1/incidents" 201 "$incident_body")"
incident_id="$(printf '%s' "$incident" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p' | head -n 1)"
if [ -z "$incident_id" ]; then
  echo "ERROR: No pude leer el id del incidente creado." >&2
  printf '%s\n' "$incident" >&2
  exit 1
fi

status_body='{"status":"RESOLVED","resolutionNote":"Resuelto por smoke test"}'
request "resolver incidente" PATCH "/api/v1/incidents/$incident_id/status" 200 "$status_body" >/dev/null

echo ""
echo "Smoke test REST completado en $BASE_URL"
