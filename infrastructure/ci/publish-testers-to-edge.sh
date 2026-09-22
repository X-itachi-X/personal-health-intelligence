#!/bin/sh
# Full tester release: backend JAR + web app + Android APK → edge.
#
# Usage:
#   PUBLIC_URL=https://edge-server.xxx.ts.net ./infrastructure/ci/publish-testers-to-edge.sh
#   SKIP_APK=1 ...   # backend + web only (local quick deploy)
#
# CI:
#   ./infrastructure/ci/ci-release.sh

set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
EDGE_HOST="${EDGE_HOST:-edge}"

if [ -z "${PUBLIC_URL:-}" ]; then
  PUBLIC_URL=$(ssh "$EDGE_HOST" 'cat /etc/phi/public-url 2>/dev/null' || true)
fi
if [ -z "${PUBLIC_URL:-}" ]; then
  echo "Set PUBLIC_URL to your edge HTTPS URL." >&2
  exit 1
fi

export PUBLIC_URL

echo "========== 1/3 Backend =========="
"$ROOT/infrastructure/ci/publish-to-edge.sh"

echo ""
echo "========== 2/3 Web =========="
"$ROOT/infrastructure/ci/publish-web-to-edge.sh"

echo ""
echo "========== 3/3 Android APK =========="
ssh "$EDGE_HOST" "mkdir -p /var/phi/public/downloads"
scp "$ROOT/infrastructure/edge/downloads-index.html" "$EDGE_HOST:/var/phi/public/downloads/index.html"

if [ "${SKIP_APK:-}" = "1" ]; then
  echo "SKIP_APK=1 — APK not built (dev only)."
elif [ -n "${APK:-}" ] && [ -f "$APK" ]; then
  RELEASE_NOTES="${RELEASE_NOTES:-}" "$ROOT/infrastructure/ci/publish-apk-to-edge.sh" "$APK"
elif [ "${BUILD_APK:-}" = "1" ] || [ "${CI:-}" = "true" ]; then
  RELEASE_NOTES="${RELEASE_NOTES:-}" "$ROOT/infrastructure/ci/build-android-apk-ci.sh"
else
  echo "ERROR: APK is required for tester releases." >&2
  echo "  BUILD_APK=1 $0" >&2
  echo "  APK=path/to.apk $0" >&2
  echo "  SKIP_APK=1 $0   # backend + web only" >&2
  exit 1
fi

echo ""
echo "========== Live =========="
echo "Web:       ${PUBLIC_URL}/"
echo "APK page:  ${PUBLIC_URL}/downloads/"
echo "API:       ${PUBLIC_URL}/api/v1/health"
