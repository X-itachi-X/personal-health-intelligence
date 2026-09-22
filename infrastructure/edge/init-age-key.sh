#!/bin/sh
# Generate age keypair on edge (run once).
# Prints the public key — use it on your dev machine with encrypt-env.sh.

set -euo pipefail

KEY=/etc/phi/age.key

run_root() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    doas "$@"
  fi
}

if [ -f "$KEY" ]; then
  echo "Age key already exists at $KEY"
  echo "Public key:"
  run_root age-keygen -y "$KEY"
  exit 0
fi

echo "Generating age keypair at $KEY (root-only)..."
run_root mkdir -p /etc/phi
run_root age-keygen -o "$KEY"
run_root chmod 600 "$KEY"
run_root chown root:root "$KEY"

echo ""
echo "Public key (save this on your dev machine as infrastructure/edge/phi.age.pub):"
run_root age-keygen -y "$KEY"
