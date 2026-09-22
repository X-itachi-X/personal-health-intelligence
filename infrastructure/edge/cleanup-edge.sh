#!/bin/sh
# One-time production cleanup after Option B bootstrap.
# Run on edge: sh /opt/phi-edge/cleanup-edge.sh

set -euo pipefail

EDGE=/opt/phi-edge

if [ -x "$EDGE/ensure-runtime-deps.sh" ]; then
  sh "$EDGE/ensure-runtime-deps.sh"
fi

run_root() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    doas "$@"
  fi
}

echo "==> Stop Gradle daemon (legacy build-on-edge)"
pkill -f 'GradleDaemon' 2>/dev/null || true

echo "==> Remove application source tree and deploy staging"
run_root rm -rf /opt/phi
rm -rf ~/phi-edge-bootstrap /tmp/phi-deploy.jar

echo "==> Trim Gradle caches under dev home"
rm -rf "$HOME/.gradle/daemon" "$HOME/.gradle/caches" 2>/dev/null || true

if [ ! -f /etc/phi/env.age ] && [ -f /etc/phi/env ]; then
  echo "==> Encrypt secrets at rest"
  sh "$EDGE/migrate-to-encrypted-env.sh"
else
  echo "==> Secrets already encrypted or no plain env to migrate"
fi

echo "==> Install boot hook (auto-start on reboot)"
sh "$EDGE/install-boot-hook.sh"

echo "==> Install nightly backup cron"
sh "$EDGE/install-cron.sh"

if apk info -e openjdk21-jdk >/dev/null 2>&1; then
  echo "==> Remove JDK — JRE is enough for running phi.jar"
  run_root apk del openjdk21-jdk || true
fi

if [ -f "$EDGE/phi.openrc" ]; then
  echo "==> Refresh OpenRC unit"
  run_root cp "$EDGE/phi.openrc" /etc/init.d/phi
  run_root chmod +x /etc/init.d/phi
fi

sh "$EDGE/boot-services.sh"

echo ""
echo "==> Cleanup complete"
echo "  /opt/phi          — removed"
echo "  /opt/phi-edge/    — ops scripts only"
echo "  /etc/local.d/     — phi.start boot hook"
if [ -f /etc/phi/env.age ]; then
  echo "  /etc/phi/env.age  — encrypted secrets"
fi
du -sh /var/phi/data /var/phi/reports 2>/dev/null || true
