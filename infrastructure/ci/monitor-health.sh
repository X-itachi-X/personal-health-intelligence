#!/bin/sh
# Ping PHI health API every second — logs to infrastructure/ci/logs/health-monitor/
#
# Usage:
#   ./infrastructure/ci/monitor-health.sh
#   LOCAL_URL=http://192.168.1.71 ./infrastructure/ci/monitor-health.sh

set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
EDGE_HOST="${EDGE_HOST:-edge}"
PUBLIC_URL="${PUBLIC_URL:-}"
LOCAL_URL="${LOCAL_URL:-http://192.168.1.71}"
INTERVAL="${INTERVAL:-1}"
TIMEOUT="${TIMEOUT:-4}"
RETRIES="${RETRIES:-3}"
LOG_DIR="${LOG_DIR:-$ROOT/infrastructure/ci/logs/health-monitor}"

if [ -z "$PUBLIC_URL" ]; then
  PUBLIC_URL=$(ssh -o ConnectTimeout=5 "$EDGE_HOST" 'cat /etc/phi/public-url 2>/dev/null' || true)
fi
if [ -z "$PUBLIC_URL" ]; then
  PUBLIC_URL="https://edge-server.tail8a02ee.ts.net"
fi

LAN_URL="${LOCAL_URL%/}/api/v1/health"
PUBLIC_HEALTH="${PUBLIC_URL%/}/api/v1/health"

mkdir -p "$LOG_DIR"
SESSION=$(date '+%Y%m%d-%H%M%S')
LOG_FILE="$LOG_DIR/monitor-$SESSION.log"
LATEST="$LOG_DIR/latest.log"
PID_FILE="$LOG_DIR/monitor.pid"

log_line() {
  line="$1"
  echo "$line"
  echo "$line" >>"$LOG_FILE"
}

if [ -f "$PID_FILE" ]; then
  old=$(cat "$PID_FILE" 2>/dev/null || true)
  if [ -n "$old" ] && kill -0 "$old" 2>/dev/null; then
    log_line "$(date '+%Y-%m-%d %H:%M:%S') WARN  another monitor running (pid $old) — starting anyway"
  fi
fi
echo $$ >"$PID_FILE"

check_url() {
  url="$1"
  err_file=$(mktemp)
  start_ms=$(date +%s%3N 2>/dev/null || echo 0)
  body=$(curl -fsS --max-time "$TIMEOUT" "$url" 2>"$err_file") || body=""
  end_ms=$(date +%s%3N 2>/dev/null || echo 0)
  err=$(head -1 "$err_file" 2>/dev/null | tr -d '\n')
  rm -f "$err_file"

  if echo "$body" | grep -q '"status":"ok"'; then
    if [ "$start_ms" != "0" ] && [ "$end_ms" != "0" ]; then
      ms=$((end_ms - start_ms))
      echo "OK ${ms}ms"
    else
      echo "OK"
    fi
  elif [ -n "$err" ]; then
    echo "FAIL $err"
  else
    echo "FAIL no response"
  fi
}

log_line "$(date '+%Y-%m-%d %H:%M:%S') START session=$SESSION"
log_line "$(date '+%Y-%m-%d %H:%M:%S') INFO  LAN=$LAN_URL"
log_line "$(date '+%Y-%m-%d %H:%M:%S') INFO  PUBLIC=$PUBLIC_HEALTH"
log_line "$(date '+%Y-%m-%d %H:%M:%S') INFO  interval=${INTERVAL}s timeout=${TIMEOUT}s retries=$RETRIES"
log_line "$(date '+%Y-%m-%d %H:%M:%S') INFO  log=$LOG_FILE"
log_line "----------------------------------------"

cp "$LOG_FILE" "$LATEST" 2>/dev/null || ln -sf "$LOG_FILE" "$LATEST" 2>/dev/null || true

down_since=""
consecutive_fail=0

while true; do
  ts=$(date '+%Y-%m-%d %H:%M:%S')
  lan_result=$(check_url "$LAN_URL")
  pub_result=$(check_url "$PUBLIC_HEALTH")

  lan_ok=0
  pub_ok=0
  echo "$lan_result" | grep -q '^OK' && lan_ok=1
  echo "$pub_result" | grep -q '^OK' && pub_ok=1

  if [ "$lan_ok" -eq 1 ] || [ "$pub_ok" -eq 1 ]; then
    if [ -n "$down_since" ]; then
      log_line "$ts  UP     recovered (down since $down_since) lan=$lan_result public=$pub_result"
      down_since=""
    else
      log_line "$ts  OK     lan=$lan_result public=$pub_result"
    fi
    consecutive_fail=0
  else
    consecutive_fail=$((consecutive_fail + 1))
    if [ -z "$down_since" ]; then
      down_since="$ts"
      log_line "$ts  DOWN   lan=$lan_result public=$pub_result"
    elif [ $((consecutive_fail % 10)) -eq 0 ]; then
      log_line "$ts  DOWN   still waiting ${consecutive_fail}s lan=$lan_result public=$pub_result"
    fi
  fi

  cp "$LOG_FILE" "$LATEST" 2>/dev/null || true
  sleep "$INTERVAL"
done
