#!/bin/sh
# Run this ON THE EDGE SERVER in an interactive SSH session (doas needs a password).
# Line-by-line guide: documentation/edge-first-deploy-walkthrough.md

set -euo pipefail

REPO_HOME="${REPO_HOME:-$HOME/personal-health-intelligence}"
OPT_DIR=/opt/phi

echo "==> [1/8] Copy repo to $OPT_DIR (needs doas password)"
doas mkdir -p "$OPT_DIR"
doas chown -R dev:dev "$OPT_DIR"
cp -a "$REPO_HOME/." "$OPT_DIR/"

echo "==> [2/8] One-time setup (packages, dirs, OpenRC, Caddy)"
doas sh "$OPT_DIR/infrastructure/edge/setup.sh"

echo "==> [3/8] Encrypt production secrets (age — no plain text on disk)"
if [ -f /etc/phi/env.age ]; then
  echo "Encrypted env already installed at /etc/phi/env.age"
elif [ -f "$HOME/phi.env.staging" ] || [ -f /etc/phi/env ]; then
  sh "$OPT_DIR/infrastructure/edge/migrate-to-encrypted-env.sh"
else
  echo "Missing ~/phi.env.staging — create and run migrate-to-encrypted-env.sh" >&2
  exit 1
fi

echo "==> [4/8] Configure Caddy (LAN HTTP — PHI_DOMAIN empty)"
sh "$OPT_DIR/infrastructure/edge/configure-proxy.sh"

echo "==> [5/8] Build JAR and start API"
sh "$OPT_DIR/infrastructure/edge/deploy.sh"

echo "==> [6/8] Install nightly backup cron"
sh "$OPT_DIR/infrastructure/edge/install-cron.sh"

echo "==> [7/8] Smoke test"
sh "$OPT_DIR/infrastructure/edge/verify.sh"

echo "==> [8/8] Service status"
doas rc-service phi status
doas rc-service caddy status

echo ""
echo "Done. API should be at http://192.168.1.71/api/v1/health"
echo "Set mobile EXPO_PUBLIC_API_URL=http://192.168.1.71 in mobile/.env"
