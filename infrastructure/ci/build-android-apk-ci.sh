#!/bin/sh
# Build Android APK in CI or release pipeline, then publish to edge.
#
# Requires one of:
#   EXPO_TOKEN  — EAS cloud build (recommended for GitHub Actions)
#   Android SDK — local Gradle build (USE_LOCAL_BUILD path)
#
# Env:
#   PUBLIC_URL       — edge API URL baked into the APK
#   APP_VERSION_CODE — android.versionCode (default: GITHUB_RUN_NUMBER or bump)
#   RELEASE_NOTES    — shown on downloads page + manifest

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
export EXPO_PUBLIC_API_URL="$PUBLIC_URL"

VERSION_CODE="${APP_VERSION_CODE:-${GITHUB_RUN_NUMBER:-}}"
if [ -n "$VERSION_CODE" ]; then
  "$ROOT/infrastructure/ci/set-app-version-code.sh" "$VERSION_CODE"
else
  export BUMP_VERSION=1
fi

cd "$ROOT/mobile"

if [ -n "${EXPO_TOKEN:-}" ]; then
  echo "==> EAS Android APK (CI, API=$PUBLIC_URL)"
  npm ci
  npx eas-cli build --platform android --profile preview --non-interactive --wait
  APK_OUT=$(mktemp -d)/phi-release.apk
  npx eas-cli build:download --platform android --latest --output "$APK_OUT"
  RELEASE_CHANNEL="${RELEASE_CHANNEL:-testers}" RELEASE_NOTES="${RELEASE_NOTES:-}" "$ROOT/infrastructure/ci/publish-apk-to-edge.sh" "$APK_OUT"
  exit 0
fi

if [ -d "${ANDROID_HOME:-}" ] || [ -d "${ANDROID_SDK_ROOT:-}" ] || [ -d "$HOME/Android/Sdk" ]; then
  echo "==> Local Gradle APK (Android SDK detected)"
  USE_LOCAL_BUILD=1 RELEASE_CHANNEL="${RELEASE_CHANNEL:-testers}" RELEASE_NOTES="${RELEASE_NOTES:-}" "$ROOT/infrastructure/ci/build-android-apk.sh"
  exit 0
fi

echo "APK build failed: set EXPO_TOKEN (EAS) or install Android SDK on the runner." >&2
exit 1
