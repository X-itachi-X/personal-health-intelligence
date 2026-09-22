#!/bin/sh
# One-time edge-server setup (run from /opt/phi clone as dev, with doas where noted).

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

echo "Creating PHI directories..."
doas mkdir -p /var/phi/data /var/phi/reports /var/phi/backups /etc/phi
doas chown -R dev:dev /var/phi

echo "Secrets: use encrypted env only (no plain /etc/phi/env)."
echo "  1. sh $SCRIPT_DIR/init-age-key.sh"
echo "  2. On dev: encrypt-env.sh your.env phi.age.pub → scp phi.env.age to /etc/phi/env.age"
echo "  Or migrate existing plain env: sh $SCRIPT_DIR/migrate-to-encrypted-env.sh"

echo "Installing runtime dependencies..."
# JDK (not JRE) — Gradle needs javac to build bootJar on the edge box
# age — encrypt secrets at rest (/etc/phi/env.age)
doas apk add --no-cache git openjdk21-jdk caddy curl age tesseract-ocr tesseract-ocr-data-eng

echo "Installing OpenRC services..."
doas cp "$SCRIPT_DIR/phi.openrc" /etc/init.d/phi
doas chmod +x /etc/init.d/phi
doas rc-update add phi default

doas mkdir -p /etc/caddy
sh "$SCRIPT_DIR/configure-proxy.sh"
doas rc-update add caddy default

chmod +x "$SCRIPT_DIR"/*.sh

echo ""
echo "Setup complete. Next steps:"
echo "  1. Encrypt secrets: init-age-key.sh + encrypt-env.sh (see documentation/edge-secrets.md)"
echo "  2. Set PHI_DOMAIN for public HTTPS, then: sh $SCRIPT_DIR/configure-proxy.sh"
echo "  3. Build and deploy: sh $SCRIPT_DIR/deploy.sh"
echo "  4. Install nightly backups: sh $SCRIPT_DIR/install-cron.sh"
echo "  5. Smoke test: sh $SCRIPT_DIR/verify.sh"
