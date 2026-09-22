#!/bin/sh
# Build JAR locally (or in CI) and deploy to edge — Option B, no source on prod.
#
# Usage:
#   EDGE_HOST=edge ./infrastructure/ci/publish-to-edge.sh
#   EDGE_HOST=dev@192.168.1.71 ./infrastructure/ci/publish-to-edge.sh
#
# Requires: ssh/scp access to edge, JDK 21 locally for build.

set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
EDGE_HOST="${EDGE_HOST:-edge}"
EDGE_SCRIPTS="${EDGE_SCRIPTS:-/opt/phi-edge}"
REMOTE_JAR=/tmp/phi-deploy.jar

cd "$ROOT/backend"

if [ -z "${JAVA_HOME:-}" ] && [ -d /usr/lib/jvm/java-21-openjdk ]; then
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
fi

echo "==> Building bootJar"
./gradlew --stop 2>/dev/null || true
./gradlew bootJar -x test

JAR=$(ls -1 build/libs/backend-*.jar | grep -v plain | head -1)
echo "==> Built $JAR"

echo "==> Sync ops scripts to $EDGE_HOST:$EDGE_SCRIPTS"
ssh "$EDGE_HOST" "mkdir -p $EDGE_SCRIPTS"
scp "$ROOT/infrastructure/edge/deploy-artifact.sh" \
    "$ROOT/infrastructure/edge/verify.sh" \
    "$ROOT/infrastructure/edge/decrypt-env.sh" \
    "$ROOT/infrastructure/edge/env.sh" \
    "$EDGE_HOST:$EDGE_SCRIPTS/"

echo "==> Upload JAR"
scp "$JAR" "$EDGE_HOST:$REMOTE_JAR"

echo "==> Install and restart"
ssh "$EDGE_HOST" "chmod +x $EDGE_SCRIPTS/*.sh && sh $EDGE_SCRIPTS/deploy-artifact.sh $REMOTE_JAR"

echo "==> Done. Test: curl http://192.168.1.71/api/v1/health"
