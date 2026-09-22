#!/bin/sh
# Load PHI environment — encrypted at rest via age (see decrypt-env.sh).

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
# shellcheck source=decrypt-env.sh
. "$SCRIPT_DIR/decrypt-env.sh"

# Back-compat aliases used by deploy/configure scripts
read_env_var() {
  read_phi_env_var "$1"
}

load_env_file() {
  load_phi_env
}
