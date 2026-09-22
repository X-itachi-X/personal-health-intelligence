#!/bin/sh
# Install the correct Caddyfile (LAN HTTP vs public TLS) from /etc/phi/env.
# Run after editing PHI_DOMAIN: sh /opt/phi/infrastructure/edge/configure-proxy.sh

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ENV_FILE=/etc/phi/env
CADDY_DEST=/etc/caddy/Caddyfile

# shellcheck source=env.sh
. "$SCRIPT_DIR/env.sh"

if [ ! -f /etc/phi/env.age ] && [ ! -f "$ENV_FILE" ]; then
  echo "Missing /etc/phi/env.age — run init-age-key.sh and encrypt-env.sh first." >&2
  exit 1
fi

PHI_DOMAIN=$(read_env_var PHI_DOMAIN)

if [ -n "${PHI_DOMAIN:-}" ]; then
  echo "Configuring Caddy for TLS domain: $PHI_DOMAIN"
  sed "s/__PHI_DOMAIN__/$PHI_DOMAIN/g" "$SCRIPT_DIR/Caddyfile.tls" | doas tee "$CADDY_DEST" >/dev/null
  doas chmod 644 "$CADDY_DEST"
else
  echo "PHI_DOMAIN unset — using LAN HTTP proxy on :80"
  doas cp "$SCRIPT_DIR/Caddyfile.lan" "$CADDY_DEST"
  doas chown root:root "$CADDY_DEST"
  doas chmod 644 "$CADDY_DEST"
fi

if command -v rc-service >/dev/null 2>&1; then
  doas rc-service caddy reload 2>/dev/null || doas rc-service caddy restart
else
  echo "Reload Caddy manually after editing $CADDY_DEST"
fi

echo "Caddy proxy configured."
