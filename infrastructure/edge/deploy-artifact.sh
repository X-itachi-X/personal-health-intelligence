#!/bin/sh
# Install a pre-built JAR on edge — no git, no Gradle, no source tree.
# Called by infrastructure/ci/publish-to-edge.sh or CI after scp.
#
# Usage: sh deploy-artifact.sh /tmp/phi-deploy.jar

set -euo pipefail

JAR_SRC="${1:?path to uploaded JAR required}"
JAR_DEST=/var/phi/phi.jar
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if [ ! -f "$JAR_SRC" ]; then
  echo "JAR not found: $JAR_SRC" >&2
  exit 1
fi

echo "Installing $(basename "$JAR_SRC") → $JAR_DEST"
cp "$JAR_SRC" "$JAR_DEST"
rm -f "$JAR_SRC"

if command -v rc-service >/dev/null 2>&1; then
  doas rc-service phi restart
else
  echo "OpenRC not found — start java manually: java -jar $JAR_DEST" >&2
  exit 1
fi

if [ -x "$SCRIPT_DIR/verify.sh" ]; then
  sh "$SCRIPT_DIR/verify.sh"
else
  curl -fsS http://localhost:8080/api/v1/health >/dev/null
fi

echo "Artifact deploy complete."
