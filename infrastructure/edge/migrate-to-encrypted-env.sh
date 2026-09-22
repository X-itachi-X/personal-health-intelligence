#!/bin/sh
# Migrate legacy plain /etc/phi/env → encrypted /etc/phi/env.age, then delete plain file.
# Run on edge in interactive SSH (needs doas).

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if [ -x "$SCRIPT_DIR/ensure-runtime-deps.sh" ]; then
  sh "$SCRIPT_DIR/ensure-runtime-deps.sh"
elif ! command -v age-keygen >/dev/null 2>&1; then
  doas apk add --no-cache age
fi

PLAIN=/etc/phi/env
ENCRYPTED=/etc/phi/env.age
KEY=/etc/phi/age.key
STAGING="$HOME/phi.env.staging"

if [ -f "$ENCRYPTED" ]; then
  echo "Encrypted env already exists at $ENCRYPTED"
  exit 0
fi

if [ ! -f "$PLAIN" ] && [ ! -f "$STAGING" ]; then
  echo "No plain env found at $PLAIN or $STAGING" >&2
  exit 1
fi

if [ ! -f "$KEY" ]; then
  sh "$SCRIPT_DIR/init-age-key.sh"
fi

SOURCE="$PLAIN"
if [ ! -f "$SOURCE" ]; then
  SOURCE="$STAGING"
fi

run_root() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    doas "$@"
  fi
}

PUB=$(run_root age-keygen -y "$KEY")
echo "Encrypting $SOURCE → $ENCRYPTED"
run_root sh -c "age -e -r '$PUB' -o '$ENCRYPTED' '$SOURCE'"
run_root chmod 644 "$ENCRYPTED"
run_root chown root:root "$ENCRYPTED"

if [ -f "$PLAIN" ]; then
  run_root rm -f "$PLAIN"
  echo "Removed plain $PLAIN"
fi

if [ -f "$STAGING" ]; then
  rm -f "$STAGING"
  echo "Removed staging $STAGING"
fi

echo "Done. Secrets are now encrypted at rest in $ENCRYPTED"
