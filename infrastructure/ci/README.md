# CI/CD — Option B (JAR-only production)

**Production edge server does not keep application source code.**  
Only the built JAR, data directories, encrypted secrets, and small ops scripts.

## What lives on production (`edge`)

| Path | Contents | Notes |
|------|----------|--------|
| `/var/phi/phi.jar` | Spring Boot binary | Deployed by CI or `publish-to-edge.sh` |
| `/var/phi/data/` | H2 `phi.mv.db`, DuckDB `analytics.duckdb` | **Source of truth** — biomarkers, extracted text, users |
| `/var/phi/reports/` | Uploaded PDFs and images | **Ephemeral** — see retention below |
| `/var/phi/backups/` | Nightly DB + reports tarball | 14 days kept |
| `/etc/phi/env.age` + `age.key` | Encrypted secrets | Never in git |
| `/opt/phi-edge/` | Shell scripts only (~10 files) | No `backend/src` |

### PDFs and images

Uploads land in `/var/phi/reports/` during ingest. After text extraction, retention is controlled by **`PHI_RETAIN_FILES_DAYS`** in encrypted env:

| Value | Behavior |
|-------|----------|
| `0` (recommended prod) | PDF/image **deleted** after extract; **extracted text + biomarkers stay in H2** |
| `7` (tester phase) | Files kept 7 days for ops/debug |

So: you are **not** building a PDF archive on prod by default — you keep **structured data in H2**. Files are only on disk briefly unless you raise retention.

## What is NOT on production

- Git repository / `backend/src`
- Gradle build (optional: remove `openjdk21-jdk` after switching to JRE-only)
- `.env` plain text
- Mobile app source

## Release pipeline (backend + web + APK)

**Push to `main`** → GitHub Actions runs tests, then deploys everything to edge.

| Workflow | Trigger | What it does |
|----------|---------|--------------|
| **`release.yml`** | Push to `main`, manual | Test → deploy JAR + web + APK |
| `edge-build.yml` | Pull requests | Tests only |
| `edge-deploy.yml` | Manual | JAR-only emergency deploy |

### One-time GitHub setup

1. **Self-hosted runner** on your Fedora PC (same LAN as edge):
   ```bash
   # GitHub → Repo → Settings → Actions → Runners → New self-hosted runner
   # Follow prompts; add labels: self-hosted, edge-lan
   ```

2. **Runner prerequisites:** JDK 21, Node 22, `ssh` to edge (host alias `edge` in `~/.ssh/config`)

3. **GitHub repository secrets** (Settings → Secrets → Actions):

   | Secret | Example | Required |
   |--------|---------|----------|
   | `PUBLIC_URL` | `https://edge-server.tail8a02ee.ts.net` | Yes |
   | `EXPO_TOKEN` | From [expo.dev](https://expo.dev) → Access tokens | Yes (APK via EAS) |
   | `EDGE_HOST` | `edge` | Optional if SSH config uses `edge` |

4. **Expo / EAS** (one-time on your laptop):
   ```bash
   npm i -g eas-cli && eas login
   cd mobile && eas build:configure
   eas credentials   # Android keystore for preview profile
   ```

5. Push to `main` — workflow builds APK in EAS cloud, downloads it on the runner, uploads to edge.

### Manual release (same as CI)

```bash
PUBLIC_URL=https://edge-server.tail8a02ee.ts.net \
EXPO_TOKEN=your_token \
./infrastructure/ci/ci-release.sh
```

Backend + web only (skip APK): `SKIP_APK=1 ./infrastructure/ci/publish-testers-to-edge.sh`

### Scripts

| Script | Purpose |
|--------|---------|
| `ci-release.sh` | Full release entrypoint (CI + manual) |
| `publish-testers-to-edge.sh` | Backend + web + APK → edge |
| `publish-to-edge.sh` | Backend JAR only |
| `publish-web-to-edge.sh` | Expo web export |
| `build-android-apk-ci.sh` | EAS or local SDK → APK → edge |
| `publish-apk-to-edge.sh` | Upload APK + `android.json` manifest |

Cloud GitHub runners **cannot** SSH to your LAN edge box — the **self-hosted runner** on your PC performs deploy.

Application secrets (`CLAUDE_API_KEY`, etc.) stay **only** on edge in `env.age`.

## Migrating existing edge (had full source in `/opt/phi`)

```bash
# After new deploy works via publish-to-edge.sh
ssh edge
doas rc-service phi stop
rm -rf /opt/phi    # removes source tree — data is under /var/phi
sh /opt/phi-edge/install-prod-scripts.sh   # if scripts not yet installed
```

Data in `/var/phi/data` and `/var/phi/reports` is untouched.

## Auto-start on reboot

OpenRC services `phi` and `caddy` are in the `default` runlevel. A boot hook
(`/etc/local.d/phi.start`) runs `boot-services.sh` as a safety net after reboot.

One-time finalize on edge (interactive — one doas password):

```bash
ssh -t edge "sh /opt/phi-edge/finalize-edge.sh"
```

Manual recovery without reboot:

```bash
ssh edge "sh /opt/phi-edge/boot-services.sh"
```

### After reboot (automatic)

OpenRC starts `tailscale` → `caddy` → `phi` → `local` (runs `boot-services.sh`).

`boot-services.sh` waits for Tailscale, re-enables Funnel, verifies health. No manual steps.

One-time install: `sh /opt/phi-edge/install-boot-hook.sh`

## Public access without a domain (Tailscale Funnel)

See [documentation/public-access-without-domain.md](../../documentation/public-access-without-domain.md).

| Script | Purpose |
|--------|---------|
| `install-tailscale-funnel.sh` | Free HTTPS URL, exposes only Caddy :80 |
| `publish-web-to-edge.sh` | Build + deploy Expo web to `/var/phi/www` |
| `build-android-apk.sh` | Interactive EAS / local APK build |
| `publish-apk-to-edge.sh` | Upload APK + auto-update manifest |
