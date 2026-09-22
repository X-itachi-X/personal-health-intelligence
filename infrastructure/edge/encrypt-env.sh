#!/bin/sh
# Encrypt a plain env file for the edge server (run on your dev machine).
#
# Usage:
#   ./encrypt-env.sh path/to/plain.env path/to/phi.age.pub
#   ./encrypt-env.sh ~/phi.env.staging infrastructure/edge/phi.age.pub
#
# Output: phi.env.age (upload to edge as /etc/phi/env.age)

set -euo pipefail

PLAIN="${1:?plain env file required}"
PUB="${2:?age public key file required (from edge init-age-key.sh)}"
OUT="${3:-phi.env.age}"

if ! command -v age >/dev/null 2>&1; then
  echo "Install age on your dev machine: dnf install age  (or apk add age)" >&2
  exit 1
fi

age -e -r "$(tr -d '\n' < "$PUB")" -o "$OUT" "$PLAIN"
chmod 600 "$OUT"
echo "Wrote encrypted env: $OUT"
echo "Upload: scp $OUT edge:/tmp/env.age && ssh edge 'doas mv /tmp/env.age /etc/phi/env.age && doas chmod 644 /etc/phi/env.age'"
