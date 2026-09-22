#!/bin/sh
# Ensure Alpine packages required for PHI edge ops are present.

set -euo pipefail

run_root() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    doas "$@"
  fi
}

missing=""
for pkg in age openjdk21-jre caddy tesseract-ocr tesseract-ocr-data-eng curl; do
  if ! apk info -e "$pkg" >/dev/null 2>&1; then
    missing="$missing $pkg"
  fi
done

if [ -n "$missing" ]; then
  echo "Installing missing packages:$missing"
  run_root apk add --no-cache $missing
fi
