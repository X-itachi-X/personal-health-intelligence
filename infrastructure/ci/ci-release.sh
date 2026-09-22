#!/bin/sh
# Full edge release: backend + web + APK. Used by GitHub Actions and manual deploys.
#
# Usage (from repo root):
#   PUBLIC_URL=https://edge-server.xxx.ts.net EXPO_TOKEN=xxx ./infrastructure/ci/ci-release.sh
#
# GitHub Actions sets CI=true, GITHUB_RUN_NUMBER, and secrets automatically.

set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
export CI=true

if [ -z "${RELEASE_NOTES:-}" ]; then
  RELEASE_NOTES=$(git -C "$ROOT" log -1 --pretty=%s 2>/dev/null || echo "Release")
  export RELEASE_NOTES
fi

export BUILD_APK=1
exec "$ROOT/infrastructure/ci/publish-testers-to-edge.sh"
