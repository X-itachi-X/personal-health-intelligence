#!/bin/sh
# Build Expo web export and deploy static files to edge (served at PUBLIC_URL/).
#
# Usage:
#   PUBLIC_URL=https://edge-server.tailxxxxx.ts.net ./infrastructure/ci/publish-web-to-edge.sh
#   # Or read from edge: PUBLIC_URL=$(ssh edge cat /etc/phi/public-url) ./infrastructure/ci/publish-web-to-edge.sh

set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
EDGE_HOST="${EDGE_HOST:-edge}"
WEB_DEST=/var/phi/www

if [ -z "${PUBLIC_URL:-}" ]; then
  PUBLIC_URL=$(ssh "$EDGE_HOST" 'cat /etc/phi/public-url 2>/dev/null' || true)
fi
if [ -z "${PUBLIC_URL:-}" ]; then
  echo "Set PUBLIC_URL to your Tailscale Funnel HTTPS URL (see install-tailscale-funnel.sh)." >&2
  exit 1
fi

cd "$ROOT/mobile"
export EXPO_PUBLIC_API_URL="$PUBLIC_URL"

echo "==> Building web export (API=$PUBLIC_URL)"
npx expo export --platform web

echo "==> Upload to $EDGE_HOST:$WEB_DEST"
ssh "$EDGE_HOST" "mkdir -p $WEB_DEST"
scp -r dist/* "$EDGE_HOST:$WEB_DEST/"

echo "==> Done. Open: ${PUBLIC_URL}/"
