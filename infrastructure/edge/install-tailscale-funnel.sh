#!/bin/sh
# Expose Caddy (:80) to the public internet via Tailscale Funnel — free HTTPS, no domain.
#
# What this does:
#   - Only port 80 on THIS machine is published (API + web + APK downloads via Caddy)
#   - Does NOT expose SSH, Proxmox, or other LAN devices
#   - URL looks like: https://edge-server.your-tailnet.ts.net
#
# One-time on edge (interactive):
#   sh /opt/phi-edge/install-tailscale-funnel.sh
#
# Alternatives (see documentation/public-access-without-domain.md):
#   - Cloudflare Quick Tunnel (random trycloudflare.com URL)
#   - Tailscale only (testers join your tailnet — no public URL)

set -euo pipefail

EDGE=/opt/phi-edge

run_root() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    doas "$@"
  fi
}

echo "==> Ensure public dirs for web + APK"
run_root mkdir -p /var/phi/www /var/phi/public/downloads
run_root chown -R dev:dev /var/phi/www /var/phi/public
if [ -f "$EDGE/downloads-index.html" ]; then
  cp "$EDGE/downloads-index.html" /var/phi/public/downloads/index.html
fi

echo "==> Install / refresh Caddy routes (API + web + /downloads)"
sh "$EDGE/configure-proxy.sh"

if ! command -v tailscale >/dev/null 2>&1; then
  echo "==> Install Tailscale"
  run_root apk add --no-cache tailscale
  run_root rc-update add tailscale default 2>/dev/null || true
  run_root rc-service tailscale start
fi

if ! tailscale status >/dev/null 2>&1; then
  echo ""
  echo "Tailscale is not logged in yet."
  echo "Run on edge (open the URL in a browser):"
  echo "  tailscale up"
  echo ""
  echo "Then re-run:"
  echo "  sh $EDGE/install-tailscale-funnel.sh"
  if [ "$(id -u)" -eq 0 ] || doas -n true 2>/dev/null; then
    echo ""
    echo "Attempting tailscale up now..."
    tailscale up 2>&1 || doas tailscale up 2>&1 || true
  fi
  if ! tailscale status >/dev/null 2>&1; then
    exit 1
  fi
fi

echo "==> Enable Tailscale Funnel → local Caddy :80"
run_funnel() {
  if [ "$(id -u)" -eq 0 ]; then
    tailscale funnel --bg 80
  else
    doas tailscale funnel --bg 80
  fi
}
if ! run_funnel 2>&1; then
  echo ""
  echo "If Funnel is not enabled on your tailnet, open the link above in a browser,"
  echo "then re-run: sh $EDGE/install-tailscale-funnel.sh"
  exit 1
fi

echo ""
echo "==> Public URLs (share with testers on any network)"
tailscale funnel status 2>/dev/null || true

PUBLIC_URL=$(tailscale funnel status 2>/dev/null | grep -oE 'https://[^ ]+' | head -1 || true)
if [ -n "$PUBLIC_URL" ]; then
  run_root sh -c "echo '$PUBLIC_URL' > /etc/phi/public-url"
  run_root chmod 644 /etc/phi/public-url
  run_root sh -c 'echo enabled > /etc/phi/use-funnel'
  run_root chmod 644 /etc/phi/use-funnel
  sh "$EDGE/install-boot-hook.sh" 2>/dev/null || true
  echo ""
  echo "Saved to /etc/phi/public-url"
  echo ""
  echo "On your dev machine, build clients with:"
  echo "  PUBLIC_URL=$PUBLIC_URL ./infrastructure/ci/publish-web-to-edge.sh"
  echo "  PUBLIC_URL=$PUBLIC_URL ./infrastructure/ci/build-android-apk.sh"
  echo ""
  echo "Tester links:"
  echo "  Web:     $PUBLIC_URL/"
  echo "  APK:     $PUBLIC_URL/downloads/"
  echo "  API:     $PUBLIC_URL/api/v1/health"
else
  echo "Could not detect funnel URL — run: tailscale funnel status"
fi
