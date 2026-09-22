#!/bin/sh
# Install nightly backup cron for the dev user on Alpine.

set -euo pipefail

if [ -x /opt/phi-edge/backup.sh ]; then
  BACKUP_SCRIPT=/opt/phi-edge/backup.sh
else
  BACKUP_SCRIPT=/opt/phi/infrastructure/edge/backup.sh
fi
CRON_LINE="0 2 * * * $BACKUP_SCRIPT >> /var/phi/backup-cron.log 2>&1"

if [ ! -x "$BACKUP_SCRIPT" ]; then
  chmod +x "$BACKUP_SCRIPT"
fi

touch /var/phi/backup-cron.log 2>/dev/null || doas touch /var/phi/backup-cron.log
chmod 644 /var/phi/backup-cron.log 2>/dev/null || doas chmod 644 /var/phi/backup-cron.log

EXISTING=$(doas crontab -l 2>/dev/null || true)
if echo "$EXISTING" | grep -q "$BACKUP_SCRIPT"; then
  echo "Backup cron already installed (root crontab)."
  exit 0
fi

{
  echo "$EXISTING"
  echo "$CRON_LINE"
} | doas crontab -

echo "Installed root backup cron: $CRON_LINE"
