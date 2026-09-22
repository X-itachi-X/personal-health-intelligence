#!/bin/sh
# Register boot-services.sh to run on every reboot (Alpine OpenRC local.d).
# Run once on edge: sh /opt/phi-edge/install-boot-hook.sh

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
HOOK=/etc/local.d/phi.start

run_root() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    doas "$@"
  fi
}

if [ ! -x "$SCRIPT_DIR/boot-services.sh" ]; then
  echo "Missing $SCRIPT_DIR/boot-services.sh" >&2
  exit 1
fi

DEST=/opt/phi-edge/boot-services.sh
if [ "$SCRIPT_DIR/boot-services.sh" != "$DEST" ]; then
  run_root cp "$SCRIPT_DIR/boot-services.sh" "$DEST"
fi
run_root chmod +x "$DEST"

run_root tee "$HOOK" >/dev/null <<'EOF'
#!/bin/sh
# PHI full stack — runs after OpenRC default services (see boot-services.sh)
sleep 3
/opt/phi-edge/boot-services.sh
EOF
run_root chmod +x "$HOOK"

# Boot order: networking → tailscale → caddy → phi → local (phi.start → funnel + verify)
for svc in tailscale caddy phi local; do
  if ! rc-update show default 2>/dev/null | grep -qE "[[:space:]]${svc}[[:space:]]|${svc}[[:space:]]"; then
    run_root rc-update add "$svc" default
  fi
done

# Do not auto-start ephemeral Cloudflare Quick Tunnel
run_root rc-update del cloudflared default 2>/dev/null || true
run_root rc-service cloudflared stop 2>/dev/null || true

# Marker: boot-services should enable Tailscale Funnel after reboot
run_root sh -c 'echo enabled > /etc/phi/use-funnel'
run_root chmod 644 /etc/phi/use-funnel

echo "Boot hook installed: $HOOK"
echo "Services in default runlevel: tailscale, caddy, phi, local"
echo "Funnel auto-start: /etc/phi/use-funnel"
echo "Test without reboot: sh /opt/phi-edge/boot-services.sh"
