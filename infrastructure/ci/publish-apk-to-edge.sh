#!/bin/sh
# Upload Android APK + release manifest to edge for tester download and in-app updates.
#
# Usage:
#   PUBLIC_URL=https://edge-server.xxx.ts.net ./infrastructure/ci/publish-apk-to-edge.sh path/to/app.apk
#   RELEASE_NOTES="Feedback screen" ./infrastructure/ci/publish-apk-to-edge.sh path/to/app.apk
#   BUMP_VERSION=1 ./infrastructure/ci/publish-apk-to-edge.sh path/to/app.apk  # increments versionCode in app.json
#
# Testers:
#   Download page:  ${PUBLIC_URL}/downloads/
#   Direct APK:     ${PUBLIC_URL}/downloads/phi-latest.apk
#   Update manifest:${PUBLIC_URL}/downloads/android.json

set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
EDGE_HOST="${EDGE_HOST:-edge}"
DEST_DIR=/var/phi/public/downloads
APK_DEST="$DEST_DIR/phi-latest.apk"
MANIFEST_DEST="$DEST_DIR/android.json"
APP_JSON="$ROOT/mobile/app.json"

APK="${1:-}"
if [ -z "$APK" ]; then
  APK=$(find "$ROOT/mobile" -name '*.apk' -type f 2>/dev/null | head -1 || true)
fi
if [ -z "$APK" ] || [ ! -f "$APK" ]; then
  echo "Usage: $0 path/to/app.apk" >&2
  echo "Build first: PUBLIC_URL=... ./infrastructure/ci/build-android-apk.sh" >&2
  exit 1
fi

if [ -z "${PUBLIC_URL:-}" ]; then
  PUBLIC_URL=$(ssh "$EDGE_HOST" 'cat /etc/phi/public-url 2>/dev/null' || true)
fi
if [ -z "${PUBLIC_URL:-}" ]; then
  echo "Set PUBLIC_URL to your edge HTTPS URL." >&2
  exit 1
fi

if [ "${BUMP_VERSION:-}" = "1" ]; then
  echo "==> Bump android.versionCode in app.json"
  node - "$APP_JSON" <<'NODE'
const fs = require("fs");
const path = process.argv[1];
const app = JSON.parse(fs.readFileSync(path, "utf8"));
const android = app.expo.android ?? {};
const next = Number(android.versionCode ?? 0) + 1;
app.expo.android = { ...android, versionCode: next };
fs.writeFileSync(path, JSON.stringify(app, null, 2) + "\n");
console.log(`versionCode → ${next}`);
NODE
fi

VERSION=$(node -p "require('$APP_JSON').expo.version")
VERSION_CODE=$(node -p "require('$APP_JSON').expo.android.versionCode || 1")
SHA256=$(sha256sum "$APK" | awk '{print $1}')
PUBLISHED_AT=$(date -u +%Y-%m-%dT%H:%M:%SZ)
RELEASE_NOTES="${RELEASE_NOTES:-}"

MANIFEST=$(mktemp)
node - "$MANIFEST" "$VERSION" "$VERSION_CODE" "$PUBLIC_URL" "$SHA256" "$PUBLISHED_AT" "$RELEASE_NOTES" <<'NODE'
const fs = require("fs");
const [out, version, versionCode, publicUrl, sha256, publishedAt, releaseNotes] = process.argv.slice(2);
fs.writeFileSync(
  out,
  JSON.stringify(
    {
      version,
      versionCode: Number(versionCode),
      apkUrl: `${publicUrl}/downloads/phi-latest.apk`,
      sha256,
      publishedAt,
      releaseNotes,
    },
    null,
    2
  ) + "\n"
);
NODE

echo "==> Upload $(basename "$APK") (v$VERSION / $VERSION_CODE)"
ssh "$EDGE_HOST" "mkdir -p $DEST_DIR"
scp "$APK" "$EDGE_HOST:$APK_DEST"
scp "$MANIFEST" "$EDGE_HOST:$MANIFEST_DEST"
if [ -f "$ROOT/infrastructure/edge/downloads-index.html" ]; then
  scp "$ROOT/infrastructure/edge/downloads-index.html" "$EDGE_HOST:$DEST_DIR/index.html"
fi
ssh "$EDGE_HOST" "chmod 644 $APK_DEST $MANIFEST_DEST $DEST_DIR/index.html 2>/dev/null || chmod 644 $APK_DEST $MANIFEST_DEST"

rm -f "$MANIFEST"

echo "==> Done."
echo "Share with testers: ${PUBLIC_URL}/downloads/"
echo "Manifest:           ${PUBLIC_URL}/downloads/android.json"
