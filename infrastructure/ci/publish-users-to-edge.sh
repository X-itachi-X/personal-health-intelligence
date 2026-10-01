#!/bin/sh
# Full production release: backend JAR + web app + Android APK → edge user portal.
#
# Usage:
#   PUBLIC_URL=https://edge-server.xxx.ts.net ./infrastructure/ci/publish-users-to-edge.sh
#   SKIP_APK=1 ...   # backend + web only

set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
EDGE_HOST="${EDGE_HOST:-edge}"
export RELEASE_CHANNEL="users"

if [ -z "${PUBLIC_URL:-}" ]; then
  PUBLIC_URL=$(ssh "$EDGE_HOST" 'cat /etc/phi/public-url 2>/dev/null' || true)
fi
if [ -z "${PUBLIC_URL:-}" ]; then
  echo "Set PUBLIC_URL to your edge HTTPS URL." >&2
  exit 1
fi

PUBLIC_URL="${PUBLIC_URL%/}"
export PUBLIC_URL

echo "========== 1/3 Backend =========="
"$ROOT/infrastructure/ci/publish-to-edge.sh"

echo ""
echo "========== 2/3 Web =========="
"$ROOT/infrastructure/ci/publish-web-to-edge.sh"

echo ""
echo "========== 3/3 Android APK (Official Release) =========="
ssh "$EDGE_HOST" "mkdir -p /var/phi/public/downloads"
scp "$ROOT/infrastructure/edge/downloads-index.html" "$EDGE_HOST:/var/phi/public/downloads/index.html"

if [ "${SKIP_APK:-}" = "1" ]; then
  echo "SKIP_APK=1 — APK not built."
elif [ -n "${APK:-}" ] && [ -f "$APK" ]; then
  RELEASE_CHANNEL=users RELEASE_NOTES="${RELEASE_NOTES:-}" "$ROOT/infrastructure/ci/publish-apk-to-edge.sh" "$APK"
elif [ "${BUILD_APK:-}" = "1" ] || [ "${CI:-}" = "true" ]; then
  RELEASE_CHANNEL=users RELEASE_NOTES="${RELEASE_NOTES:-}" "$ROOT/infrastructure/ci/build-android-apk-ci.sh"
else
  # Check if there is an existing APK in mobile
  FOUND_APK=$(find "$ROOT/mobile" -name '*.apk' -type f 2>/dev/null | head -1 || true)
  if [ -n "$FOUND_APK" ] && [ -f "$FOUND_APK" ]; then
    RELEASE_CHANNEL=users RELEASE_NOTES="${RELEASE_NOTES:-}" "$ROOT/infrastructure/ci/publish-apk-to-edge.sh" "$FOUND_APK"
  else
    echo "Notice: No APK provided. Backend & Web deployed."
  fi
fi

echo ""
echo "========== Production Release Live =========="
echo "Web:            ${PUBLIC_URL}/"
echo "Download Page:  ${PUBLIC_URL}/downloads/"
echo "Manifest:       ${PUBLIC_URL}/downloads/android.json"
echo "API Health:     ${PUBLIC_URL}/api/v1/health"