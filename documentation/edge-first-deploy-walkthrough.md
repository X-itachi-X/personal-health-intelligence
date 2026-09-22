# Edge server — first deploy walkthrough (line by line)

**Server:** `ssh edge` → Alpine Linux VM at `192.168.1.71`  
**Policy:** OpenRC (no Docker) — JVM + H2 + Caddy, auto-restart on crash  
**Date prepared:** 2026-09-19

This document explains **every step** to put PHI backend into production on your edge box.

---

## What was already done for you (from your dev machine)

These ran from Fedora via `ssh edge` (no `doas` needed):

| Step | Command | What it did |
|------|---------|-------------|
| 1 | `git clone … ~/personal-health-intelligence` | First clone from GitHub (old scripts only) |
| 2 | `tar … \| scp … \| tar xzf` | **Replaced** with your **latest local code** (all edge scripts, respawn, backups, TLS) |
| 3 | Created `~/phi.env.staging` | Production env file with **new random** `PHI_JWT_SECRET` + `PHI_H2_PASSWORD`, your Claude key from local `.env`, CORS for `192.168.1.71` |

**Verify on edge:**

```bash
ls ~/personal-health-intelligence/infrastructure/edge/
# Should list: setup.sh deploy.sh backup.sh phi.openrc configure-proxy.sh verify.sh …

ls -l ~/phi.env.staging
# Should be -rw------- (mode 600)
```

---

## Architecture (what you’re installing)

```
Phone / laptop (Expo)
        │
        ▼  HTTP :80
   Caddy (reverse proxy, TLS optional later)
        │
        ▼  localhost:8080
   Java Spring Boot (phi.jar)
        ├── H2 file     /var/phi/data/phi.mv.db
        ├── DuckDB      /var/phi/data/analytics.duckdb
        └── Reports     /var/phi/reports/  (ephemeral when RETAIN=0)

OpenRC service "phi"  → starts on boot, restarts on crash (supervise-daemon)
Cron 02:00 daily      → backup.sh copies DB + reports to /var/phi/backups/
```

---

## Part A — Run setup (interactive SSH required)

`doas` on Alpine needs your **password in a real terminal**. Open:

```bash
ssh edge
```

Then run the all-in-one script:

```bash
chmod +x ~/personal-health-intelligence/infrastructure/edge/run-first-time-setup.sh
~/personal-health-intelligence/infrastructure/edge/run-first-time-setup.sh
```

Below is **what each step inside that script does**, line by line.

---

### Step 1 — Copy code to `/opt/phi`

```bash
doas mkdir -p /opt/phi
```

| Line | Meaning |
|------|---------|
| `doas` | Run as root (Alpine sudo) |
| `mkdir -p /opt/phi` | Create standard install path used by all scripts |

```bash
doas chown -R dev:dev /opt/phi
```

| Line | Meaning |
|------|---------|
| `chown -R dev:dev` | You (`dev`) own the tree so `git pull` / `gradlew` work without root |

```bash
cp -a ~/personal-health-intelligence/. /opt/phi/
```

| Line | Meaning |
|------|---------|
| `cp -a` | Copy entire repo preserving permissions |
| `/opt/phi/` | Permanent location `deploy.sh` expects |

---

### Step 2 — `setup.sh` (one-time machine prep)

```bash
doas sh /opt/phi/infrastructure/edge/setup.sh
```

Inside `setup.sh`:

| Lines | What happens |
|-------|----------------|
| `mkdir -p /var/phi/data /var/phi/reports /var/phi/backups /etc/phi` | Data directories for DB, uploads, backups |
| `chown -R dev:dev /var/phi` | App runs as `dev`, writes DB here |
| Copy `phi.env.example` → `/etc/phi/env` if missing | Default env template (you overwrite next) |
| `apk add git openjdk21-jre caddy curl tesseract-ocr …` | Java runtime, proxy, OCR for PDF ingest |
| Copy `phi.openrc` → `/etc/init.d/phi` | OpenRC service definition |
| `rc-update add phi default` | Start API on boot |
| `configure-proxy.sh` | Install LAN `Caddyfile` → `/etc/caddy/Caddyfile` |
| `rc-update add caddy default` | Start Caddy on boot |

**`phi.openrc` highlights:**

| Setting | Purpose |
|---------|---------|
| `command_args="-jar /var/phi/phi.jar"` | Run fat JAR |
| `supervisor=supervise-daemon` | Watch process — **restart if JVM crashes** |
| `respawn_delay=2` | Wait 2s before restart |
| `respawn_max=0` | Unlimited respawns |
| `start_pre` loads `/etc/phi/env` | Secrets not in git |

---

### Step 3 — Install secrets

```bash
doas cp ~/phi.env.staging /etc/phi/env
doas chmod 600 /etc/phi/env
doas chown root:root /etc/phi/env
```

| Line | Meaning |
|------|---------|
| `cp … /etc/phi/env` | Production config only root should read |
| `chmod 600` | Owner read/write only |
| Variables | See `infrastructure/edge/phi.env.example` |

**Important variables in your staging file:**

| Variable | Purpose |
|----------|---------|
| `PHI_JWT_SECRET` | Signs login tokens (random, unique to edge) |
| `PHI_H2_PASSWORD` | Encrypts/password-protects H2 file DB |
| `CLAUDE_API_KEY` | Ingest-only Claude calls |
| `PHI_CORS_ORIGINS` | Must include your phone/laptop origins hitting `http://192.168.1.71` |
| `PHI_DOMAIN` | Empty = LAN HTTP only; set later for HTTPS |

---

### Step 4 — Caddy reverse proxy

```bash
sh /opt/phi/infrastructure/edge/configure-proxy.sh
```

| Branch | Result |
|--------|--------|
| `PHI_DOMAIN` empty | Copies `Caddyfile.lan` → listen `:80`, proxy to `localhost:8080` |
| `PHI_DOMAIN` set | Copies `Caddyfile.tls` with Let's Encrypt |

Reloads Caddy so port 80 forwards to Spring Boot.

---

### Step 5 — `deploy.sh` (build + start)

```bash
sh /opt/phi/infrastructure/edge/deploy.sh
```

| Line in script | Meaning |
|----------------|---------|
| Load `/etc/phi/env` | Pick up secrets |
| Check `PHI_JWT_SECRET` / `PHI_H2_PASSWORD` not placeholders | Fail fast if misconfigured |
| `git pull origin main` | Update code (after first deploy) |
| `./gradlew bootJar -x test` | Build runnable JAR (~1–3 min first time) |
| `cp … /var/phi/phi.jar` | Install artifact |
| `rc-service phi restart` | Start/restart API |
| `curl localhost:8080/api/v1/health` | Confirm JVM is up |
| `verify.sh` | Extra smoke test |

**First run note:** `git pull` may fail if `/opt/phi` isn’t a git repo (we used `cp`). That’s OK for first deploy — build still runs. For updates later:

```bash
cd /opt/phi && git init && git remote add origin https://github.com/X-itachi-X/personal-health-intelligence.git
# or keep using scp/tar from dev machine
```

---

### Step 6 — Nightly backups

```bash
sh /opt/phi/infrastructure/edge/install-cron.sh
```

| Action | Meaning |
|--------|---------|
| Adds root crontab `0 2 * * * …/backup.sh` | Every day 02:00 |
| `backup.sh` stops `phi` ~5s | Consistent H2 file copy |
| Copies `phi.mv.db`, `analytics.duckdb`, `reports.tar.gz` | Full restore set |
| Keeps 14 folders under `/var/phi/backups/` | Auto-prune old |

Logs: `/var/log/phi-backup.log`

---

### Step 7 — Verify

```bash
sh /opt/phi/infrastructure/edge/verify.sh
```

| Check | Pass criteria |
|-------|----------------|
| `curl http://localhost:8080/api/v1/health` | Returns JSON `"status":"ok"` |
| If `PHI_DOMAIN` set | HTTPS health also works |

**From your laptop (same LAN):**

```bash
curl http://192.168.1.71/api/v1/health
```

---

## Part B — Point mobile app at edge

On your **dev machine**, edit `mobile/.env`:

```bash
EXPO_PUBLIC_API_URL=http://192.168.1.71
```

Restart Expo (`npx expo start`). Login/register should hit edge, not localhost.

---

## Part C — Day-2 operations

### Deploy a code update

```bash
ssh edge
# Option A: pull on server (if git remote configured)
sh /opt/phi/infrastructure/edge/deploy.sh

# Option B: from dev machine (when GitHub behind)
cd ~/Documents/Projects/personal-health-intelligence
tar czf /tmp/phi-edge-sync.tar.gz --exclude=.git --exclude=node_modules … .
scp /tmp/phi-edge-sync.tar.gz edge:/tmp/
ssh edge 'tar xzf /tmp/phi-edge-sync.tar.gz -C /opt/phi'
ssh edge 'sh /opt/phi/infrastructure/edge/deploy.sh'
```

### Service commands

```bash
doas rc-service phi status    # running?
doas rc-service phi restart   # after config change
doas rc-service phi stop
tail -f /var/log/phi.log      # application logs
doas rc-service caddy reload  # after Caddyfile edit
```

### Enable HTTPS later

1. Point DNS `A` record → your public IP (or use LAN DNS).
2. Open firewall ports 80 + 443.
3. Edit `/etc/phi/env`: `PHI_DOMAIN=phi.yourdomain.com`
4. `sh /opt/phi/infrastructure/edge/configure-proxy.sh`
5. Add `https://phi.yourdomain.com` to `PHI_CORS_ORIGINS`, `doas rc-service phi restart`

---

## Troubleshooting

| Symptom | Check |
|---------|-------|
| `doas: a tty is required` | Run commands in interactive `ssh edge`, not from CI/batch |
| Health fails after deploy | `tail -50 /var/log/phi.log` — often Flyway or bad env |
| Phone can’t connect | Same Wi‑Fi? Firewall on edge? CORS includes your origin? |
| H2 lock on restart | `doas rc-service phi stop`; wait 3s; `doas rc-service phi start` |
| Gradle OOM on 4G VM | `export GRADLE_OPTS="-Xmx512m"` before deploy |

---

## Security reminders

- Never commit `/etc/phi/env` or `~/phi.env.staging` to git.
- Rotate `PHI_JWT_SECRET` if leaked (forces re-login).
- Edge banner says “No Docker” — intentional for RAM and simplicity.
- PDFs are ephemeral (`PHI_RETAIN_FILES_DAYS=0`) per engineering principles.

---

## Quick checklist

- [ ] `~/personal-health-intelligence/infrastructure/edge/setup.sh` exists on edge
- [ ] `~/phi.env.staging` exists (mode 600)
- [ ] Ran `run-first-time-setup.sh` in interactive SSH
- [ ] `curl http://192.168.1.71/api/v1/health` works from laptop
- [ ] `mobile/.env` → `EXPO_PUBLIC_API_URL=http://192.168.1.71`
- [ ] Registered / logged in from phone
