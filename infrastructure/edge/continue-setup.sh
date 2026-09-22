#!/bin/sh
# Resume first-time setup after a partial run (e.g. permission error on step 4).
# Run on edge in interactive SSH: sh /opt/phi/infrastructure/edge/continue-setup.sh

set -euo pipefail

OPT_DIR=/opt/phi
EDGE_DIR="$OPT_DIR/infrastructure/edge"

echo "==> Ensure secrets are encrypted at rest"
if [ ! -f /etc/phi/env.age ]; then
  sh "$EDGE_DIR/migrate-to-encrypted-env.sh"
fi

echo "==> [4/8] Configure Caddy"
sh "$EDGE_DIR/configure-proxy.sh"

echo "==> [5/8] Build JAR and start API"
sh "$EDGE_DIR/deploy.sh"

echo "==> [6/8] Install nightly backup cron"
sh "$EDGE_DIR/install-cron.sh"

echo "==> [7/8] Smoke test"
sh "$EDGE_DIR/verify.sh"

echo "==> [8/8] Service status"
doas rc-service phi status
doas rc-service caddy status

echo ""
echo "Done. curl http://192.168.1.71/api/v1/health"
