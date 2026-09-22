#!/bin/sh
# Backup PHI database (H2), DuckDB analytics, and report files on edge-server.
# Installed via install-cron.sh: 0 2 * * * ...

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DATA_DIR=/var/phi/data
REPORTS_DIR=/var/phi/reports
BACKUP_ROOT=/var/phi/backups
STAMP=$(date +%Y%m%d_%H%M%S)
DEST="$BACKUP_ROOT/$STAMP"
LOG_TAG="[phi-backup $STAMP]"

run_root() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    doas "$@"
  fi
}

if [ -f "$SCRIPT_DIR/decrypt-env.sh" ]; then
  # shellcheck source=decrypt-env.sh
  . "$SCRIPT_DIR/decrypt-env.sh"
  load_phi_env 2>/dev/null || true
fi

mkdir -p "$DEST"

echo "$LOG_TAG starting backup to $DEST"

# Brief stop for a consistent H2 file copy (~5s downtime at 2 AM).
if command -v rc-service >/dev/null 2>&1; then
  run_root rc-service phi stop
  sleep 2
fi

if [ -f "$DATA_DIR/phi.mv.db" ]; then
  cp "$DATA_DIR/phi.mv.db" "$DEST/phi.mv.db"
fi

if [ -f "$DATA_DIR/phi.trace.db" ]; then
  cp "$DATA_DIR/phi.trace.db" "$DEST/phi.trace.db"
fi

if [ -f "$DATA_DIR/analytics.duckdb" ]; then
  cp "$DATA_DIR/analytics.duckdb" "$DEST/analytics.duckdb"
fi

if [ -d "$REPORTS_DIR" ]; then
  tar -czf "$DEST/reports.tar.gz" -C /var/phi reports
fi

if command -v rc-service >/dev/null 2>&1; then
  run_root rc-service phi start
fi

# Manifest for restore verification (no secrets).
{
  echo "timestamp=$STAMP"
  [ -f "$DEST/phi.mv.db" ] && echo "phi.mv.db=$(wc -c < "$DEST/phi.mv.db")"
  [ -f "$DEST/analytics.duckdb" ] && echo "analytics.duckdb=$(wc -c < "$DEST/analytics.duckdb")"
  [ -f "$DEST/reports.tar.gz" ] && echo "reports.tar.gz=$(wc -c < "$DEST/reports.tar.gz")"
} > "$DEST/manifest.txt"

# Keep last 14 backups
ls -1dt "$BACKUP_ROOT"/* 2>/dev/null | tail -n +15 | while IFS= read -r old; do
  [ -n "$old" ] && rm -rf "$old"
done

echo "$LOG_TAG complete"
