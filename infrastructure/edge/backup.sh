#!/bin/sh
# Backup PHI database (H2) and uploaded reports on edge-server.
# Run via cron: 0 2 * * * /opt/phi/infrastructure/edge/backup.sh

set -euo pipefail

DATA_DIR=/var/phi/data
REPORTS_DIR=/var/phi/reports
BACKUP_ROOT=/var/phi/backups
STAMP=$(date +%Y%m%d_%H%M%S)
DEST="$BACKUP_ROOT/$STAMP"

mkdir -p "$DEST"

if [ -f "$DATA_DIR/phi.mv.db" ]; then
  cp "$DATA_DIR/phi.mv.db" "$DEST/phi.mv.db"
fi

if [ -d "$REPORTS_DIR" ]; then
  tar -czf "$DEST/reports.tar.gz" -C /var/phi reports
fi

# Keep last 14 daily backups
ls -1dt "$BACKUP_ROOT"/* 2>/dev/null | tail -n +15 | xargs -r rm -rf

echo "Backup saved to $DEST"
