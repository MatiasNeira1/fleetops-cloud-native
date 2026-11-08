#!/bin/sh
set -eu

if [ "${#SIMULATOR_ADMIN_TOKEN}" -lt 32 ]; then
  echo 'SIMULATOR_ADMIN_TOKEN no configurado' >&2
  exit 2
fi

action="${1:-status}"
trip="${2:-}"
base='http://127.0.0.1:8089/api/v1/simulations'
method='GET'
body=''

case "$action" in
  status) path='' ;;
  start|pause|resume|stop) method='POST'; path="/$action" ;;
  demo) method='POST'; path='/demo/start' ;;
  trip-start|trip-pause|trip-resume|trip-stop|trip-restart|speed|halt|low-fuel|refuel)
    case "$trip" in
      ''|*[!a-zA-Z0-9_-]*) echo 'tripId invalido' >&2; exit 2 ;;
    esac
    method='POST'
    case "$action" in
      trip-start) path="/$trip/start" ;;
      trip-pause) path="/$trip/pause" ;;
      trip-resume) path="/$trip/resume" ;;
      trip-stop) path="/$trip/stop" ;;
      trip-restart) path="/$trip/restart" ;;
      speed) path="/$trip/scenario"; body='{"scenario":"SPEED_LIMIT_EXCEEDED"}' ;;
      halt) path="/$trip/scenario"; body='{"scenario":"STOP"}' ;;
      low-fuel) path="/$trip/scenario"; body='{"scenario":"PREPARE_LOW_FUEL"}' ;;
      refuel)
        case "${3:-}" in
          ''|*[!0-9.]*) echo 'Porcentaje invalido' >&2; exit 2 ;;
        esac
        path="/$trip/refuel"
        body="{\"percent\":${3}}"
        ;;
    esac
    ;;
  *) echo 'Uso: simctl.sh status|start|pause|resume|stop|demo|trip-start|trip-pause|trip-resume|trip-stop|trip-restart|speed|halt|low-fuel|refuel [tripId] [porcentaje]' >&2; exit 2 ;;
esac

if [ -n "$body" ]; then
  printf 'header = "Authorization: Bearer %s"\n' "$SIMULATOR_ADMIN_TOKEN" \
    | curl --silent --show-error --fail-with-body --config - \
        --request "$method" --header 'Content-Type: application/json' \
        --data "$body" "$base$path"
else
  printf 'header = "Authorization: Bearer %s"\n' "$SIMULATOR_ADMIN_TOKEN" \
    | curl --silent --show-error --fail-with-body --config - \
        --request "$method" "$base$path"
fi
printf '\n'
