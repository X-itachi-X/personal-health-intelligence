#!/bin/sh
# Upload Android APK + release manifest to edge with version archiving & history.
# Supports 'testers' and 'users' (production) channels.
#
# Channels:
#   testers:
#     Download page:  ${PUBLIC_URL}/downloads/testers/
#     Direct APK:     ${PUBLIC_URL}/downloads/testers/phi-latest.apk
#     Manifest:       ${PUBLIC_URL}/downloads/testers/android.json
#     Archive APKs:   ${PUBLIC_URL}/downloads/testers/archive/phi-test-vX.Y.Z-bN.apk
#   users (production):
#     Download page:  ${PUBLIC_URL}/downloads/
#     Direct APK:     ${PUBLIC_URL}/downloads/phi-latest.apk
#     Manifest:       ${PUBLIC_URL}/downloads/android.json
#     Archive APKs:   ${PUBLIC_URL}/downloads/archive/phi-vX.Y.Z-bN.apk

set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
EDGE_HOST="${EDGE_HOST:-edge}"
RELEASE_CHANNEL="${RELEASE_CHANNEL:-testers}"
APP_JSON="$ROOT/mobile/app.json"

if [ "$RELEASE_CHANNEL" = "testers" ] || [ "$RELEASE_CHANNEL" = "test" ] || [ "$RELEASE_CHANNEL" = "preview" ]; then
  CHANNEL="testers"
  DEST_DIR="/var/phi/public/downloads/testers"
  URL_PATH="/downloads/testers"
  INDEX_HTML="$ROOT/infrastructure/edge/tester-downloads-index.html"
  FILE_PREFIX="phi-test"
else
  CHANNEL="users"
  DEST_DIR="/var/phi/public/downloads"
  URL_PATH="/downloads"
  INDEX_HTML="$ROOT/infrastructure/edge/downloads-index.html"
  FILE_PREFIX="phi"
fi

APK="${1:-}"
if [ -z "$APK" ]; then
  APK=$(find "$ROOT/mobile" -name '*.apk' -type f 2>/dev/null | head -1 || true)
fi
if [ -z "$APK" ] || [ ! -f "$APK" ]; then
  echo "Usage: $0 path/to/app.apk" >&2
  exit 1
fi

if [ -z "${PUBLIC_URL:-}" ]; then
  PUBLIC_URL=$(ssh "$EDGE_HOST" 'cat /etc/phi/public-url 2>/dev/null' || true)
fi
if [ -z "${PUBLIC_URL:-}" ]; then
  echo "Set PUBLIC_URL to your edge HTTPS URL." >&2
  exit 1
fi
PUBLIC_URL="${PUBLIC_URL%/}"

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

ARCHIVE_FILENAME="${FILE_PREFIX}-v${VERSION}-b${VERSION_CODE}.apk"
LATEST_FILENAME="phi-latest.apk"
ARCHIVE_URL="${PUBLIC_URL}${URL_PATH}/archive/${ARCHIVE_FILENAME}"
LATEST_URL="${PUBLIC_URL}${URL_PATH}/${LATEST_FILENAME}"

echo "==> Updating manifest for [$CHANNEL] (v$VERSION / build $VERSION_CODE)"
EXISTING_MANIFEST=$(ssh "$EDGE_HOST" "cat $DEST_DIR/android.json 2>/dev/null" || true)

MANIFEST_TMP=$(mktemp)
node - "$MANIFEST_TMP" "$EXISTING_MANIFEST" "$CHANNEL" "$VERSION" "$VERSION_CODE" "$LATEST_URL" "$ARCHIVE_URL" "$SHA256" "$PUBLISHED_AT" "$RELEASE_NOTES" <<'NODE'
const fs = require("fs");
const [outFile, rawExisting, channel, version, codeStr, apkUrl, archiveUrl, sha256, publishedAt, releaseNotes] = process.argv.slice(2);
const versionCode = Number(codeStr);
let history = [];

if (rawExisting && rawExisting.trim().length > 0) {
  try {
    const prev = JSON.parse(rawExisting);
    const prevHist = Array.isArray(prev.history) ? prev.history : [];
    if (prev.versionCode && Number(prev.versionCode) !== versionCode) {
      const prevEntry = {
        version: prev.version || "unknown",
        versionCode: Number(prev.versionCode),
        apkUrl: prev.archiveUrl || prev.apkUrl,
        sha256: prev.sha256 || "",
        publishedAt: prev.publishedAt || "",
        releaseNotes: prev.releaseNotes || "",
      };
      history = [prevEntry, ...prevHist.filter(i => Number(i.versionCode) !== prevEntry.versionCode)];
    } else {
      history = prevHist;
    }
  } catch (_) {}
}
history = history.slice(0, 25);

const manifest = { channel, version, versionCode, apkUrl, archiveUrl, sha256, publishedAt, releaseNotes, history };
fs.writeFileSync(outFile, JSON.stringify(manifest, null, 2) + "\n");
NODE

echo "==> Syncing files to $EDGE_HOST:$DEST_DIR"
ssh "$EDGE_HOST" "mkdir -p $DEST_DIR/archive"
scp "$APK" "$EDGE_HOST:$DEST_DIR/archive/$ARCHIVE_FILENAME"
scp "$APK" "$EDGE_HOST:$DEST_DIR/$LATEST_FILENAME"
scp "$MANIFEST_TMP" "$EDGE_HOST:$DEST_DIR/android.json"

if [ -f "$INDEX_HTML" ]; then
  scp "$INDEX_HTML" "$EDGE_HOST:$DEST_DIR/index.html"
fi

ssh "$EDGE_HOST" "chmod -R 755 $DEST_DIR && chmod 644 $DEST_DIR/*.json $DEST_DIR/*.html $DEST_DIR/*.apk $DEST_DIR/archive/*.apk 2>/dev/null || true"
rm -f "$MANIFEST_TMP"

echo "==> Published [$CHANNEL] successfully"
echo "Download page:  ${PUBLIC_URL}${URL_PATH}/"
echo "Direct APK:     ${LATEST_URL}"
echo "Archived APK:   ${ARCHIVE_URL}"
