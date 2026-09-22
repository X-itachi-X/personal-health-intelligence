#!/bin/sh
# Set android.versionCode in mobile/app.json (used by CI for monotonic build numbers).
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
APP_JSON="$ROOT/mobile/app.json"
CODE="${1:-}"

if [ -z "$CODE" ]; then
  echo "Usage: $0 <versionCode>" >&2
  exit 1
fi

node - "$APP_JSON" "$CODE" <<'NODE'
const fs = require("fs");
const [file, code] = process.argv.slice(2);
const app = JSON.parse(fs.readFileSync(file, "utf8"));
app.expo.android = { ...(app.expo.android ?? {}), versionCode: Number(code) };
fs.writeFileSync(file, JSON.stringify(app, null, 2) + "\n");
console.log(`android.versionCode → ${code}`);
NODE
