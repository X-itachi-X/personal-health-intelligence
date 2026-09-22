#!/bin/sh
# One-time: install edge ops scripts only (no application source) on production.
# Run on edge: sh install-prod-scripts.sh
#
# Installs to /opt/phi-edge/ — deploy-artifact, verify, backup helpers, OpenRC unit.

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEST=/opt/phi-edge

echo "Installing production ops scripts to $DEST"
doas mkdir -p "$DEST"
doas chown -R dev:dev "$DEST"

for f in deploy-artifact.sh verify.sh backup.sh install-cron.sh configure-proxy.sh \
  decrypt-env.sh env.sh migrate-to-encrypted-env.sh init-age-key.sh start-service.sh \
  bootstrap-option-b.sh boot-services.sh install-boot-hook.sh cleanup-edge.sh \
  install-doas-policy.sh finalize-edge.sh ensure-runtime-deps.sh encrypt-env.sh \
  install-tailscale-funnel.sh fix-tailscale-public-url.sh update-cors-origins.sh \
  install-cloudflare-tunnel.sh \
  downloads-index.html \
  Caddyfile.lan Caddyfile.tls phi.openrc; do
  if [ -f "$SCRIPT_DIR/$f" ]; then
    cp "$SCRIPT_DIR/$f" "$DEST/"
  fi
done
chmod +x "$DEST"/*.sh

doas cp "$SCRIPT_DIR/phi.openrc" /etc/init.d/phi
doas chmod +x /etc/init.d/phi
doas rc-update add phi default 2>/dev/null || true

echo "Done. Production layout:"
echo "  /var/phi/phi.jar     — deployed JAR only"
echo "  /var/phi/data/       — H2 + DuckDB"
echo "  /var/phi/reports/    — uploads (retention per PHI_RETAIN_FILES_DAYS)"
echo "  /opt/phi-edge/       — ops scripts (this install)"
echo ""
echo "Deploy from dev/CI: infrastructure/ci/publish-to-edge.sh"
