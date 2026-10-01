#!/bin/sh
# Tester preview release: Android preview APK + manifest → edge tester portal.
#
# Usage:
#   PUBLIC_URL=https://edge-server.xxx.ts.net ./infrastructure/ci/publish-testers-to-edge.sh
#   APK=path/to/test.apk ./infrastructure/ci/publish-testers-to-edge.sh
#   BUILD_APK=1 ./infrastructure/ci/publish-testers-to-edge.sh

set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
EDGE_HOST="${EDGE_HOST:-edge}"
export RELEASE_CHANNEL="testers"

if [ -z "${PUBLIC_URL:-}" ]; then
  PUBLIC_URL=$(ssh "$EDGE_HOST" 'cat /etc/phi/public-url 2>/dev/null' || true)
fi
if [ -z "${PUBLIC_URL:-}" ]; then
  echo "Set PUBLIC_URL to your edge HTTPS URL." >&2
  exit 1
fi

PUBLIC_URL="${PUBLIC_URL%/}"
export PUBLIC_URL

echo "========== Deploying Tester Preview =========="
ssh "$EDGE_HOST" "mkdir -p /var/phi/public/downloads/testers"

if [ -n "${APK:-}" ] && [ -f "$APK" ]; then
  RELEASE_CHANNEL=testers RELEASE_NOTES="${RELEASE_NOTES:-}" "$ROOT/infrastructure/ci/publish-apk-to-edge.sh" "$APK"
elif [ "${BUILD_APK:-}" = "1" ] || [ "${CI:-}" = "true" ]; then
  RELEASE_CHANNEL=testers RELEASE_NOTES="${RELEASE_NOTES:-}" "$ROOT/infrastructure/ci/build-android-apk-ci.sh"
else
  # Check if there is an existing APK in mobile
  FOUND_APK=$(find "$ROOT/mobile" -name '*.apk' -type f 2>/dev/null | head -1 || true)
  if [ -n "$FOUND_APK" ] && [ -f "$FOUND_APK" ]; then
    RELEASE_CHANNEL=testers RELEASE_NOTES="${RELEASE_NOTES:-}" "$ROOT/infrastructure/ci/publish-apk-to-edge.sh" "$FOUND_APK"
  else
    echo "ERROR: No APK found. Build one first or specify APK=/path/to.apk or BUILD_APK=1." >&2
    exit 1
  fi
fi

echo ""
echo "========== Tester Release Live =========="
echo "Tester Portal:   ${PUBLIC_URL}/downloads/testers/"
echo "Tester Manifest: ${PUBLIC_URL}/downloads/testers/android.json"
echo "Tester APK:      ${PUBLIC_URL}/downloads/testers/phi-latest.apk"
