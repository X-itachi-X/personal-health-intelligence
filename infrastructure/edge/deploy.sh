#!/bin/sh
# Legacy: build on edge (requires full git clone + JDK).
# Preferred production path: infrastructure/ci/publish-to-edge.sh (JAR only, no source on prod).
set -euo pipefail

APP_DIR=/opt/phi
JAR_DEST=/var/phi/phi.jar
ENV_FILE=/etc/phi/env
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

# shellcheck source=env.sh
. "$SCRIPT_DIR/env.sh"
load_env_file "$ENV_FILE" || true

missing=""
for var in PHI_JWT_SECRET PHI_H2_PASSWORD; do
  eval "value=\${$var:-}"
  if [ -z "$value" ] || [ "$value" = "change-me-to-a-long-random-string-min-32-chars" ] || [ "$value" = "change-me-strong-h2-password" ]; then
    missing="$missing $var"
  fi
done

if [ -n "$missing" ]; then
  echo "Set production secrets in /etc/phi/env.age before deploy:$missing" >&2
  exit 1
fi

if [ ! -f /etc/phi/env.age ] && [ -f /etc/phi/env ]; then
  echo "WARNING: plain-text /etc/phi/env detected — run migrate-to-encrypted-env.sh" >&2
fi

cd "$APP_DIR"
if [ -d .git ]; then
  git pull origin main
else
  echo "No .git in $APP_DIR — skipping pull (scp/tar deploy)."
fi

if ! command -v javac >/dev/null 2>&1; then
  echo "JDK required to build on edge. Run: doas apk add openjdk21-jdk" >&2
  echo "Or build on your dev machine and copy build/libs/backend-*.jar to /var/phi/phi.jar" >&2
  exit 1
fi

# Alpine: Gradle daemon may have started under JRE before JDK was installed.
if [ -z "${JAVA_HOME:-}" ] && [ -d /usr/lib/jvm/java-21-openjdk ]; then
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
fi
export PATH="$JAVA_HOME/bin:$PATH"

cd backend
./gradlew --stop 2>/dev/null || true
./gradlew bootJar -x test

JAR=$(ls -1 build/libs/backend-*.jar | grep -v plain | head -1)
cp "$JAR" "$JAR_DEST"

doas rc-service phi restart
sleep 3

if ! curl -fsS http://localhost:8080/api/v1/health >/dev/null; then
  echo "Deploy finished but health check failed — see /var/log/phi.log" >&2
  exit 1
fi

echo "Deployed $(basename "$JAR") and restarted phi service."
sh "$SCRIPT_DIR/verify.sh"
