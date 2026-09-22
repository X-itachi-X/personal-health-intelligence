#!/bin/sh
# Add origins to PHI_CORS_ORIGINS in encrypted env and restart phi.
# Usage: sh /opt/phi-edge/update-cors-origins.sh https://edge-server.tail8a02ee.ts.net

set -euo pipefail

EDGE=/opt/phi-edge
NEW_ORIGIN="${1:-}"

run_root() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    doas "$@"
  fi
}

if [ -z "$NEW_ORIGIN" ] && [ -f /etc/phi/public-url ]; then
  NEW_ORIGIN=$(cat /etc/phi/public-url)
fi
if [ -z "$NEW_ORIGIN" ]; then
  echo "Usage: $0 <origin-url>" >&2
  exit 1
fi

# shellcheck source=decrypt-env.sh
. "$EDGE/decrypt-env.sh"
CURRENT=$(read_phi_env_var PHI_CORS_ORIGINS || true)

if echo ",$CURRENT," | grep -q ",$NEW_ORIGIN,"; then
  echo "Origin already present: $NEW_ORIGIN"
  exit 0
fi

if [ -n "$CURRENT" ]; then
  UPDATED="$CURRENT,$NEW_ORIGIN"
else
  UPDATED="$NEW_ORIGIN"
fi

STAGING=$(mktemp)
run_root sh -c "age -d -i /etc/phi/age.key /etc/phi/env.age" >"$STAGING"

if grep -q '^PHI_CORS_ORIGINS=' "$STAGING"; then
  sed -i "s|^PHI_CORS_ORIGINS=.*|PHI_CORS_ORIGINS=$UPDATED|" "$STAGING"
else
  echo "PHI_CORS_ORIGINS=$UPDATED" >>"$STAGING"
fi

PUB=$(run_root age-keygen -y /etc/phi/age.key)
run_root sh -c "age -e -r '$PUB' -o /etc/phi/env.age '$STAGING'"
run_root chmod 644 /etc/phi/env.age
rm -f "$STAGING"

echo "Updated PHI_CORS_ORIGINS:"
echo "  $UPDATED"
run_root rc-service phi restart
sleep 12
sh "$EDGE/verify.sh"
echo "Done."
