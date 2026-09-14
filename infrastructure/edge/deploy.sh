#!/bin/sh
set -euo pipefail

APP_DIR=/opt/phi
JAR_DEST=/var/phi/phi.jar

cd "$APP_DIR"
git pull origin main

cd backend
./gradlew bootJar -x test

JAR=$(ls -1 build/libs/backend-*.jar | grep -v plain | head -1)
doas cp "$JAR" "$JAR_DEST"
doas chown dev:dev "$JAR_DEST"

doas rc-service phi restart

echo "Deployed $(basename "$JAR") and restarted phi service."
