#!/bin/sh
# Fix static public URL: https://edge-server.TAILNET.ts.net
# Requires one-time Tailscale admin setup (free, no bought domain).
#
# Run on edge: sh /opt/phi-edge/fix-tailscale-public-url.sh

set -euo pipefail

EDGE=/opt/phi-edge
HOSTNAME=$(tailscale status --json 2>/dev/null | grep -oE '"DNSName": "[^"]+' | head -1 | cut -d'"' -f4 | sed 's/\.$//' || true)
if [ -z "$HOSTNAME" ]; then
  HOSTNAME=$(tailscale status 2>/dev/null | awk 'NR==2{print $1}' || echo "edge-server")
  HOSTNAME="${HOSTNAME}.tail8a02ee.ts.net"
fi
PUBLIC_URL="https://${HOSTNAME}"

run_root() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    doas "$@"
  fi
}

echo "Target static URL: $PUBLIC_URL"
echo ""

echo "==> Provision HTTPS cert for $HOSTNAME"
if ! run_root tailscale cert "$HOSTNAME" 2>&1 | grep -q 'Wrote public cert'; then
  echo "HTTPS cert not ready yet."
  echo "Confirm MagicDNS + HTTPS are enabled at https://login.tailscale.com/admin/dns"
  echo "Then: doas rc-service tailscale restart && sh $0"
  exit 1
fi

echo "==> Caddy routes"
sh "$EDGE/configure-proxy.sh"

echo "==> Stop ephemeral Cloudflare tunnel (if any)"
pkill -f 'cloudflared tunnel' 2>/dev/null || true
run_root rc-service cloudflared stop 2>/dev/null || true
run_root rc-update del cloudflared default 2>/dev/null || true

echo "==> Reset and enable Tailscale Funnel on :80"
run_root tailscale funnel --https=443 off 2>/dev/null || true
run_root tailscale funnel reset 2>/dev/null || true
sleep 2
run_root tailscale funnel --bg 80
run_root tailscale funnel status

echo ""
echo "==> Waiting for public DNS (up to 3 minutes)..."
ok=0
for i in $(seq 1 18); do
  if dig +short "$HOSTNAME" @8.8.8.8 2>/dev/null | grep -qE '^[0-9a-fA-F:.]+'; then
    ok=1
    break
  fi
  echo "  attempt $i/18 — not in public DNS yet..."
  sleep 10
done

if [ "$ok" -eq 0 ]; then
  echo ""
  echo "Public DNS still empty. Confirm HTTPS is enabled in admin console,"
  echo "wait 10 minutes, then re-run this script."
  exit 1
fi

run_root sh -c "echo '$PUBLIC_URL' > /etc/phi/public-url"
run_root chmod 644 /etc/phi/public-url
run_root sh -c 'echo enabled > /etc/phi/use-funnel'
run_root chmod 644 /etc/phi/use-funnel
sh "$EDGE/install-boot-hook.sh" 2>/dev/null || true

if curl -fsS "${PUBLIC_URL}/api/v1/health" >/dev/null 2>&1; then
  echo ""
  echo "SUCCESS — static public URL:"
  echo "  $PUBLIC_URL"
  echo ""
  echo "  Web:  ${PUBLIC_URL}/"
  echo "  APK:  ${PUBLIC_URL}/downloads/"
else
  echo "DNS OK but HTTPS check failed — wait a few minutes and test in browser."
fi
