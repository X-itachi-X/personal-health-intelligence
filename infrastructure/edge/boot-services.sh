#!/bin/sh
# Idempotent post-boot recovery — API, Caddy, Tailscale Funnel.
# Runs from /etc/local.d/phi.start on every reboot (no manual steps).
#
# Manual: sh /opt/phi-edge/boot-services.sh

EDGE=/opt/phi-edge
LOG=/var/phi/boot.log
USE_FUNNEL=/etc/phi/use-funnel
PUBLIC_URL_FILE=/etc/phi/public-url

log() {
  echo "$(date '+%Y-%m-%dT%H:%M:%S%z') $*" | tee -a "$LOG"
}

run_root() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    doas "$@"
  fi
}

run_svc() {
  svc="$1"
  action="$2"
  run_root rc-service "$svc" "$action"
}

svc_started() {
  rc-service "$1" status 2>/dev/null | grep -q 'started'
}

wait_for() {
  desc="$1"
  max="${2:-30}"
  delay="${3:-2}"
  check="$4"
  n=1
  while [ "$n" -le "$max" ]; do
    if eval "$check"; then
      return 0
    fi
    sleep "$delay"
    n=$((n + 1))
  done
  log "boot-services: timeout waiting for $desc"
  return 1
}

tailscale_hostname() {
  if command -v tailscale >/dev/null 2>&1; then
    h=$(tailscale status --json 2>/dev/null | grep -oE '"DNSName": "[^"]+' | head -1 | cut -d'"' -f4 | sed 's/\.$//' || true)
    if [ -n "$h" ]; then
      echo "$h"
      return 0
    fi
  fi
  if [ -f "$PUBLIC_URL_FILE" ]; then
    sed 's|https://||' "$PUBLIC_URL_FILE"
    return 0
  fi
  return 1
}

ensure_funnel() {
  [ -f "$USE_FUNNEL" ] || [ -f "$PUBLIC_URL_FILE" ] || return 0
  command -v tailscale >/dev/null 2>&1 || return 0

  if tailscale funnel status 2>/dev/null | grep -q 'Funnel on'; then
    log "boot-services: Tailscale Funnel already on"
    return 0
  fi

  host=$(tailscale_hostname) || {
    log "boot-services: cannot determine Tailscale hostname for Funnel"
    return 1
  }

  log "boot-services: provisioning cert for $host"
  run_root tailscale cert "$host" >/dev/null 2>&1 || true

  log "boot-services: enabling Tailscale Funnel → Caddy :80"
  run_root tailscale funnel --bg 80 || return 1
  log "boot-services: Funnel enabled"
}

log "boot-services: begin"

if [ ! -f /var/phi/phi.jar ]; then
  log "boot-services: missing /var/phi/phi.jar"
  exit 1
fi

run_root mkdir -p /var/phi/data /var/phi/reports /var/phi/backups /var/phi/www /var/phi/public/downloads
run_root chown -R dev:dev /var/phi/data /var/phi/reports /var/phi/backups /var/phi/www /var/phi/public 2>/dev/null || true
touch /var/phi/app.log 2>/dev/null || run_root touch /var/phi/app.log

# Legacy ephemeral tunnel — never auto-start on boot
if svc_started cloudflared; then
  log "boot-services: stopping cloudflared (use Tailscale Funnel)"
  run_svc cloudflared stop || true
fi

if command -v tailscale >/dev/null 2>&1; then
  if ! svc_started tailscale; then
    log "boot-services: starting tailscale"
    run_svc tailscale start
  fi
  wait_for "tailscale login" 45 2 "tailscale status >/dev/null 2>&1" || true
fi

if ! svc_started caddy; then
  log "boot-services: starting caddy"
  run_svc caddy start
fi
wait_for "caddy :80" 20 1 "curl -fsS http://127.0.0.1/api/v1/health >/dev/null 2>&1 || curl -fsS http://127.0.0.1:80/ >/dev/null 2>&1" || true

if ! svc_started phi; then
  log "boot-services: starting phi"
  run_svc phi start
fi
wait_for "phi :8080" 30 2 "curl -fsS http://127.0.0.1:8080/api/v1/health >/dev/null 2>&1" || true

ensure_funnel || log "boot-services: Funnel not ready (will retry on next boot or run fix-tailscale-public-url.sh)"

if [ -x "$EDGE/verify.sh" ]; then
  export PHI_VERIFY_TRIES=30
  export PHI_VERIFY_DELAY=2
  if sh "$EDGE/verify.sh"; then
    log "boot-services: health OK"
  else
    log "boot-services: health check failed — see /var/phi/app.log"
  fi
fi

if [ -f "$PUBLIC_URL_FILE" ]; then
  log "boot-services: public URL $(cat "$PUBLIC_URL_FILE")"
fi

log "boot-services: complete"
