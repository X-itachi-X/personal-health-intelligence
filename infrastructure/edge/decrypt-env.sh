#!/bin/sh
# Decrypt /etc/phi/env.age with /etc/phi/age.key — never store plain secrets on disk.

ENCRYPTED_ENV=/etc/phi/env.age
AGE_KEY=/etc/phi/age.key
LEGACY_PLAIN_ENV=/etc/phi/env

phi_decrypt_env_to_stdout() {
  if [ -f "$ENCRYPTED_ENV" ] && [ -f "$AGE_KEY" ]; then
    if [ "$(id -u)" -eq 0 ]; then
      age -d -i "$AGE_KEY" "$ENCRYPTED_ENV"
    else
      doas age -d -i "$AGE_KEY" "$ENCRYPTED_ENV"
    fi
    return 0
  fi

  if [ -f "$LEGACY_PLAIN_ENV" ]; then
    echo "WARNING: Using legacy plain-text $LEGACY_PLAIN_ENV — run migrate-to-encrypted-env.sh" >&2
    if [ -r "$LEGACY_PLAIN_ENV" ]; then
      cat "$LEGACY_PLAIN_ENV"
    else
      doas cat "$LEGACY_PLAIN_ENV"
    fi
    return 0
  fi

  echo "No encrypted env at $ENCRYPTED_ENV (or legacy $LEGACY_PLAIN_ENV)." >&2
  return 1
}

load_phi_env() {
  set -a
  # shellcheck disable=SC1090
  . <(phi_decrypt_env_to_stdout)
  set +a
}

read_phi_env_var() {
  var="$1"
  phi_decrypt_env_to_stdout 2>/dev/null | grep -E "^${var}=" | head -1 | cut -d= -f2- | sed 's/^"//;s/"$//'
}
