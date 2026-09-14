#!/bin/sh
# One-time edge-server setup (run as dev, with doas where noted).

set -euo pipefail

echo "Creating PHI directories..."
doas mkdir -p /var/phi/data /var/phi/reports /var/phi/backups
doas chown -R dev:dev /var/phi

echo "Installing runtime dependencies..."
doas apk add git openjdk21-jre

echo "Done. Next steps:"
echo "  1. Clone repo to /opt/phi"
echo "  2. Copy infrastructure/edge/phi.openrc to /etc/init.d/phi"
echo "  3. Set CLAUDE_API_KEY and SPRING_PROFILES_ACTIVE=prod in /etc/init.d/phi"
echo "  4. doas rc-update add phi default && doas rc-service phi start"
