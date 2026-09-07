#!/bin/sh
set -euo pipefail

APP_DIR=/opt/phi
cd "$APP_DIR"

git pull origin main
cd backend
./gradlew bootJar -x test

# openrc example — adjust for your edge-server init system
# doas cp build/libs/backend-*.jar /var/phi/phi.jar
# doas rc-service phi restart

echo "Build complete. Restart phi service manually if needed."
