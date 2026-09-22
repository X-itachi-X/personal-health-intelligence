#!/bin/sh
# Switch from manual/no-op deploy to OpenRC-managed phi service.
# Run on edge: sh /opt/phi/infrastructure/edge/start-service.sh

set -euo pipefail

if [ -d /opt/phi-edge ]; then
  EDGE_DIR=/opt/phi-edge
else
  EDGE_DIR=/opt/phi/infrastructure/edge
fi

if [ ! -f /var/phi/phi.jar ]; then
  echo "Missing /var/phi/phi.jar — run deploy.sh first." >&2
  exit 1
fi

echo "==> Install updated OpenRC unit (logs to /var/phi/app.log)"
doas cp "$EDGE_DIR/phi.openrc" /etc/init.d/phi

echo "==> Stop any manual java -jar process"
pkill -f '/var/phi/phi.jar' 2>/dev/null || true
sleep 2

echo "==> Start phi via OpenRC (auto-restart on crash)"
doas rc-service phi restart
sleep 5

sh "$EDGE_DIR/verify.sh"
