#!/bin/sh
# Build an Android APK for testers (no Play Store).
#
# Option A — EAS cloud build (recommended, needs free expo.dev account):
#   npm i -g eas-cli && eas login && eas build:configure
#   PUBLIC_URL=https://your.ts.net ./infrastructure/ci/build-android-apk.sh
#
# Option B — local prebuild + gradle (needs Android SDK):
#   USE_LOCAL_BUILD=1 PUBLIC_URL=... ./infrastructure/ci/build-android-apk.sh

set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
EDGE_HOST="${EDGE_HOST:-edge}"

if [ -z "${PUBLIC_URL:-}" ]; then
  PUBLIC_URL=$(ssh "$EDGE_HOST" 'cat /etc/phi/public-url 2>/dev/null' || true)
fi
if [ -z "${PUBLIC_URL:-}" ]; then
  echo "Set PUBLIC_URL to your Tailscale Funnel HTTPS URL." >&2
  exit 1
fi

cd "$ROOT/mobile"
export EXPO_PUBLIC_API_URL="$PUBLIC_URL"

if [ "${USE_LOCAL_BUILD:-}" = "1" ]; then
  echo "==> Local Android release APK"
  npx expo prebuild --platform android --clean
  cd android
  ./gradlew assembleRelease
  APK=$(find app/build/outputs/apk/release -name '*.apk' | head -1)
  echo "Built: $APK"
  cd "$ROOT"
  BUMP_VERSION=1 RELEASE_CHANNEL="${RELEASE_CHANNEL:-testers}" RELEASE_NOTES="${RELEASE_NOTES:-}" "$ROOT/infrastructure/ci/publish-apk-to-edge.sh" "$APK"
  exit 0
fi

if ! command -v eas >/dev/null 2>&1; then
  echo "Install EAS CLI: npm i -g eas-cli && eas login" >&2
  exit 1
fi

echo "==> EAS Android APK build (API=$PUBLIC_URL)"
EXPO_PUBLIC_API_URL="$PUBLIC_URL" eas build --platform android --profile preview --non-interactive

echo ""
echo "When the build finishes, download the APK from expo.dev, then:"
echo "  ./infrastructure/ci/publish-apk-to-edge.sh ~/Downloads/your-build.apk"
echo ""
echo "Push JS-only updates without a new APK:"
echo "  cd mobile && eas update --channel preview --message 'your change'"
