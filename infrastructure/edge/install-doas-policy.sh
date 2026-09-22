#!/bin/sh
# Optional: allow dev to manage PHI services without re-entering doas password.
# Run once interactively on edge: sh /opt/phi-edge/install-doas-policy.sh
#
# After this, cleanup-edge.sh and publish-to-edge deploys work over SSH without a TTY.

set -euo pipefail

CONF=/etc/doas.d/phi-edge.conf

if [ "$(id -u)" -ne 0 ]; then
  doas sh "$0"
  exit 0
fi

cat >"$CONF" <<'EOF'
# PHI edge maintenance — dev user on home LAN server
permit nopass keepenv dev as root cmd rc-service
permit nopass keepenv dev as root cmd rc-update
permit nopass keepenv dev as root cmd mkdir
permit nopass keepenv dev as root cmd cp
permit nopass keepenv dev as root cmd rm
permit nopass keepenv dev as root cmd chmod
permit nopass keepenv dev as root cmd chown
permit nopass keepenv dev as root cmd tee
permit nopass keepenv dev as root cmd touch
permit nopass keepenv dev as root cmd age
permit nopass keepenv dev as root cmd age-keygen
permit nopass keepenv dev as root cmd crontab
permit nopass keepenv dev as root cmd apk
permit nopass keepenv dev as root cmd tailscale
permit nopass keepenv dev as root cmd sh
EOF

chmod 644 "$CONF"
echo "Installed $CONF"
echo "Test: doas rc-service phi status"
