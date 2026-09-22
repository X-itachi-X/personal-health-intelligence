#!/bin/sh
# TEMPORARY fallback only — Quick Tunnel URL CHANGES on every restart.
# For a stable URL use: sh /opt/phi-edge/fix-tailscale-public-url.sh
#
# Public HTTPS without a bought domain — Cloudflare Quick Tunnel (free).
#
# Run on edge: sh /opt/phi-edge/install-cloudflare-tunnel.sh

set -euo pipefail

EDGE=/opt/phi-edge
LOG=/var/phi/cloudflared.log
PIDFILE=/var/phi/cloudflared.pid
URLFILE=/etc/phi/public-url

run_root() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    doas "$@"
  fi
}

echo "==> Ensure Caddy serves API + web + downloads on :80"
sh "$EDGE/configure-proxy.sh"
run_root mkdir -p /var/phi/www /var/phi/public/downloads

if ! command -v cloudflared >/dev/null 2>&1; then
  echo "==> Install cloudflared"
  run_root apk add --no-cache cloudflared
fi

echo "==> Stop any existing cloudflared"
pkill -f 'cloudflared tunnel' 2>/dev/null || true
sleep 1

echo "==> Start Quick Tunnel → http://127.0.0.1:80"
: >"$LOG"
nohup cloudflared tunnel --no-autoupdate --url http://127.0.0.1:80 >>"$LOG" 2>&1 &
echo $! >"$PIDFILE"

echo "==> Waiting for public URL..."
PUBLIC_URL=""
for _ in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
  sleep 2
  PUBLIC_URL=$(grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' "$LOG" | head -1 || true)
  if [ -n "$PUBLIC_URL" ]; then
    break
  fi
done

if [ -z "$PUBLIC_URL" ]; then
  echo "Could not read tunnel URL. Check: tail -20 $LOG" >&2
  exit 1
fi

run_root sh -c "echo '$PUBLIC_URL' > '$URLFILE'"
run_root chmod 644 "$URLFILE"

run_root tee /etc/init.d/cloudflared >/dev/null <<'EOF'
#!/sbin/openrc-run

name="cloudflared"
description="Cloudflare Quick Tunnel to local Caddy :80"
command="/usr/bin/cloudflared"
command_args="tunnel --no-autoupdate --url http://127.0.0.1:80"
command_background=true
pidfile="/var/phi/cloudflared.pid"
output_log="/var/phi/cloudflared.log"
error_log="/var/phi/cloudflared.log"

depend() {
    need net caddy
    after caddy
}
EOF
run_root chmod +x /etc/init.d/cloudflared
run_root rc-update add cloudflared default 2>/dev/null || true

echo ""
echo "==> Public URL (works from any network, no domain needed):"
echo "  $PUBLIC_URL"
echo ""
echo "  Web:      ${PUBLIC_URL}/"
echo "  APK:      ${PUBLIC_URL}/downloads/"
echo "  API:      ${PUBLIC_URL}/api/v1/health"
echo ""
echo "Saved to $URLFILE"
echo ""
echo "Rebuild clients:"
echo "  PUBLIC_URL=$PUBLIC_URL ./infrastructure/ci/publish-web-to-edge.sh"
