#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
COMPOSE_FILE="$REPO_ROOT/platform/local/docker-compose.yml"
ENV_FILE="$REPO_ROOT/.env"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: Docker no esta disponible en PATH." >&2
  exit 1
fi

echo "WARN: esto detendra el entorno local y eliminara volumenes Docker asociados al compose local." >&2

set -- compose
if [ -f "$ENV_FILE" ]; then
  set -- "$@" --env-file "$ENV_FILE"
fi
set -- "$@" -f "$COMPOSE_FILE" down -v --remove-orphans

cd "$REPO_ROOT"
docker "$@"
