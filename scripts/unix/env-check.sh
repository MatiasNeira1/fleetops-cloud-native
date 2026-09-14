#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_EXAMPLE="$REPO_ROOT/.env.example"
ENV_FILE="$REPO_ROOT/.env"

if [ ! -f "$ENV_EXAMPLE" ]; then
  echo "ERROR: falta .env.example en la raiz del repositorio." >&2
  exit 1
fi

echo "OK: .env.example existe."

if [ -f "$ENV_FILE" ]; then
  echo "OK: .env local existe y no debe versionarse."
else
  echo "WARN: .env no existe. Copia .env.example a .env si necesitas personalizar valores locales." >&2
fi
