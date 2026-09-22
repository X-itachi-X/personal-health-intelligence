# Edge secrets — encrypted at rest (age)

Plain-text `/etc/phi/env` is **not supported** for production. Secrets live in an **age-encrypted** file; only the JVM process receives decrypted values in memory at startup.

## Files on the edge server

| Path | Contents | Permissions |
|------|----------|-------------|
| `/etc/phi/age.key` | **Private** age key | `600` root:root |
| `/etc/phi/env.age` | **Encrypted** env blob | `644` root:root |
| `/etc/phi/env` | ❌ must not exist after migration | — |

The encrypted file can be copied between machines; it is useless without `age.key`.

## One-time migration (you already have plain env)

On edge (`ssh edge`):

```bash
doas apk add age
sh /opt/phi/infrastructure/edge/migrate-to-encrypted-env.sh
```

| Step | What happens |
|------|----------------|
| 1 | Creates `/etc/phi/age.key` if missing |
| 2 | Encrypts `/etc/phi/env` → `/etc/phi/env.age` |
| 3 | **Deletes** plain `/etc/phi/env` and `~/phi.env.staging` |
| 4 | Prints public key for future re-encryption from dev |

Then reinstall OpenRC unit and restart:

```bash
doas cp /opt/phi/infrastructure/edge/phi.openrc /etc/init.d/phi
doas rc-service phi restart
```

## New server (no plain env ever)

**On edge:**

```bash
sh /opt/phi/infrastructure/edge/init-age-key.sh
```

Copy the printed public key to your dev machine as `infrastructure/edge/phi.age.pub`.

**On dev machine:**

```bash
cd personal-health-intelligence
# Edit a local file from phi.env.example — never commit it
cp infrastructure/edge/phi.env.example /tmp/phi-prod.env
vi /tmp/phi-prod.env

dnf install age   # or: apk add age
chmod +x infrastructure/edge/encrypt-env.sh
./infrastructure/edge/encrypt-env.sh /tmp/phi-prod.env infrastructure/edge/phi.age.pub phi.env.age

scp phi.env.age edge:/tmp/
ssh edge 'doas mv /tmp/env.age /etc/phi/env.age 2>/dev/null || doas mv /tmp/phi.env.age /etc/phi/env.age; doas chmod 644 /etc/phi/env.age'
rm -f /tmp/phi-prod.env phi.env.age
```

## How decryption works

```
/etc/phi/env.age  +  /etc/phi/age.key
         │                    │
         └────── age -d ──────┘
                    │
                    ▼ (pipe, never written to disk)
            OpenRC start_pre / deploy.sh
                    │
                    ▼
            java -jar phi.jar (env in process memory only)
```

Scripts: `infrastructure/edge/decrypt-env.sh`

## Rotating secrets

1. Decrypt locally (temporary): `ssh edge 'doas age -d -i /etc/phi/age.key /etc/phi/env.age' > /tmp/phi.env`
2. Edit values
3. Re-encrypt with `encrypt-env.sh` and upload new `env.age`
4. `doas rc-service phi restart`

Or edit on edge via: `doas sh -c 'age -d -i /etc/phi/age.key /etc/phi/env.age > /tmp/e && vi /tmp/e'` then re-encrypt in place with `migrate` logic.

## What this does *not* do

- **Disk encryption** (LUKS) — separate Proxmox/host concern
- **Hide secrets from root** — root can always decrypt with `age.key`
- **Encrypt H2 database file** — use `PHI_H2_PASSWORD`; DB file is encrypted by H2 when password set

## Backup note

`backup.sh` does **not** copy `/etc/phi/env.age` or `age.key` by default. Back them up separately to a password manager or offline storage.
