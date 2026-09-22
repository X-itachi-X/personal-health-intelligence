#!/bin/sh
# Post-deploy smoke test — run on the edge server after deploy.sh or setup.

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ENV_FILE=/etc/phi/env
BASE_URL=${PHI_VERIFY_URL:-http://localhost:8080}

# shellcheck source=env.sh
. "$SCRIPT_DIR/env.sh"
PHI_DOMAIN=$(read_env_var PHI_DOMAIN || true)
if [ -n "${PHI_DOMAIN:-}" ]; then
  BASE_URL="https://${PHI_DOMAIN}"
fi

echo "Checking API health at $BASE_URL/api/v1/health ..."
TRIES=${PHI_VERIFY_TRIES:-15}
DELAY=${PHI_VERIFY_DELAY:-2}
n=1
while [ "$n" -le "$TRIES" ]; do
  if curl -fsS "$BASE_URL/api/v1/health" >/dev/null; then
    break
  fi
  if [ "$n" -eq "$TRIES" ]; then
    echo "Health check failed after ${TRIES} attempts." >&2
    exit 1
  fi
  echo "  waiting (${n}/${TRIES}) ..."
  sleep "$DELAY"
  n=$((n + 1))
done

echo "OK — API is reachable."

if [ -n "${PHI_DOMAIN:-}" ]; then
  echo "Checking TLS certificate for $PHI_DOMAIN ..."
  if ! curl -fsSI "https://${PHI_DOMAIN}/api/v1/health" >/dev/null; then
    echo "TLS check failed — confirm DNS, port 443, and Caddy logs." >&2
    exit 1
  fi
  echo "OK — HTTPS is working."
fi

if [ -d /var/phi/backups ]; then
  LATEST=$(ls -1dt /var/phi/backups/* 2>/dev/null | head -1 || true)
  if [ -n "$LATEST" ]; then
    echo "Latest backup: $LATEST"
  else
    echo "No backups yet — install cron via install-cron.sh"
  fi
fi
