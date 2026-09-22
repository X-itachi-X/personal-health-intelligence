#!/bin/sh
# One interactive session on edge: doas policy + full cleanup + boot hook.
# From laptop:  ssh -t edge "sh /opt/phi-edge/finalize-edge.sh"

set -euo pipefail

EDGE=/opt/phi-edge

echo "==> Install doas policy (one password prompt, then passwordless deploy/cleanup)"
sh "$EDGE/install-doas-policy.sh"

echo "==> Run production cleanup"
sh "$EDGE/cleanup-edge.sh"

echo ""
echo "Edge is production-ready. Reboot-safe: phi + caddy start automatically."
