#!/bin/sh
# One-time interactive bootstrap for Option B (JAR-only prod, no source tree).
# Run on edge after scripts are staged: sh ~/phi-edge-bootstrap/bootstrap-option-b.sh
#
# Needs: doas password (wheel). Data under /var/phi is untouched.

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
STAGING="${SCRIPT_DIR}"

echo "==> Install ops scripts to /opt/phi-edge"
sh "$STAGING/install-prod-scripts.sh"

if [ -f /tmp/phi-deploy.jar ]; then
  echo "==> Install staged JAR → /var/phi/phi.jar"
  cp /tmp/phi-deploy.jar /var/phi/phi.jar
  rm -f /tmp/phi-deploy.jar
fi

echo "==> Stop manual java -jar (if any)"
pkill -f '/var/phi/phi.jar' 2>/dev/null || true
sleep 2

echo "==> Start phi via OpenRC (auto-restart, logs /var/phi/app.log)"
doas rc-service phi restart

echo "==> Verify (Spring Boot may take ~15s to start)"
sh /opt/phi-edge/verify.sh

echo ""
echo "==> Option B bootstrap complete."
echo ""
echo "Optional next steps:"
echo "  1. Encrypt secrets:  sh /opt/phi-edge/migrate-to-encrypted-env.sh"
echo "  2. Nightly backups:    sh /opt/phi-edge/install-cron.sh"
echo "  3. Remove old source:  rm -rf /opt/phi   # only after publish-to-edge works"
echo ""
echo "Future deploys from laptop: ./infrastructure/ci/publish-to-edge.sh"
