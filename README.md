# Personal Health Intelligence

Turn uploaded lab reports into **structured, longitudinal health data** for families — trends, comparisons, and decision support built on a real database, not another chat box.

> **We are engineering a health data platform.** PDFs are ephemeral inputs. The database is the source of truth. Claude is used **once per report** at the ingest boundary — not as a dump-for-every-question chat wrapper. See [documentation/00-engineering-principles.md](documentation/00-engineering-principles.md).

## Stack

| Layer | Technology |
|-------|------------|
| Mobile | Expo (React Native) + TypeScript |
| Backend | Spring Boot 4 + Java 21 |
| Database | **H2** (embedded OLTP — source of truth) |
| Analytics | **DuckDB** (trends, compare — synced from H2) |
| AI (ingest only) | Claude API — unstructured text → biomarker JSON, once per report |
| Server | edge-server (Alpine, Proxmox, 4 GiB RAM) |
| Repo | [GitHub](https://github.com/X-itachi-X/personal-health-intelligence) |

## Why H2 everywhere?

Family-scale traffic, single Spring Boot process, minimal RAM on edge-server. No separate database daemon.

| Location | H2 file | Ingest files (ephemeral) |
|----------|---------|--------------------------|
| **Dev** (Fedora) | `~/.phi/h2db.mv.db` | `~/.phi/reports/` (purged after local extract by default) |
| **Prod** (edge) | `/var/phi/data/phi.mv.db` | `/var/phi/reports/` (short retention) |

## Project structure

```
personal-health-intelligence/
├── mobile/          # Expo app
├── backend/         # Spring Boot API + extraction engine
├── skills/          # Medical domain knowledge (markdown)
├── rules/           # Deterministic rules (YAML)
├── data/            # Golden test dataset
├── infrastructure/  # Caddy, deploy, backup scripts
├── documentation/   # Technical docs — start at 00-engineering-principles.md
├── sprints/         # Feature tracking (current, backlog, completed)
└── docs/            # Project notes
```

## Local development (Fedora)

### Prerequisites

- Java 21+ (`java-25-openjdk-devel`)
- Node.js 22+ and npm (for mobile)
- **Tesseract OCR** (for village scans / photos — primary user path):
  ```bash
  sudo dnf install tesseract tesseract-langpack-eng tesseract-langpack-hin
  ```
- Claude API key (only needed when rules + OCR text are not enough — optional in dev)

### Claude API setup

1. Sign up at [platform.claude.com](https://platform.claude.com) (Google or email).
2. Add billing: **Settings → Billing** — add a card and buy credits (~$5 is plenty to start).
3. Create a key: **Settings → API Keys → Create Key**. Copy it immediately (`sk-ant-...`).
4. Paste into project root `.env`:
   ```bash
   cp .env.example .env
   # edit CLAUDE_API_KEY=sk-ant-...
   ```
5. Start the backend with `./backend/run-dev.sh` (loads `.env` automatically).
6. Verify: `curl http://localhost:8080/api/v1/health` should show `"claudeConfigured": true`.

**Token discipline:** Set `PHI_CLAUDE_AUTO_PARSE=false` in `.env` during UI work — uploads save extracted text to DB without calling Claude. Trigger parse manually via `POST /api/v1/reports/{id}/parse`.

**Cost (when parsing):** ~₹5–7 per lab report with Sonnet; switch to Haiku in `application.yml` for ~₹2.5/report.

### Backend

```bash
cd backend
./run-dev.sh               # loads ../.env, dev profile + H2
# or: ./gradlew bootRun    # if CLAUDE_API_KEY is already exported
```

Health check: `curl http://localhost:8080/api/v1/health`

H2 console (dev only): http://localhost:8080/h2-console  
JDBC URL: `jdbc:h2:file:~/.phi/h2db`

### Mobile

```bash
cd mobile
npm install
npm start
# press w for web, or scan QR with Expo Go
```

Set API URL in `.env`: `EXPO_PUBLIC_API_URL=http://localhost:8080`

## Deployment (edge-server)

No PostgreSQL required — one JVM runs API + H2 + DuckDB.

```bash
# One-time setup on Alpine edge VM
doas sh /opt/phi/infrastructure/edge/setup.sh
doas vi /etc/phi/env                    # secrets — see infrastructure/edge/phi.env.example

# Optional HTTPS: set PHI_DOMAIN in /etc/phi/env, then:
sh /opt/phi/infrastructure/edge/configure-proxy.sh

# Full tester release (backend + web + APK) — manual or via CI
PUBLIC_URL=https://your-edge.ts.net EXPO_TOKEN=xxx ./infrastructure/ci/ci-release.sh

# Or push to main — GitHub Actions release.yml deploys automatically
# (requires self-hosted runner + secrets — see infrastructure/ci/README.md)

sh /opt/phi/infrastructure/edge/install-cron.sh   # nightly backups at 02:00
sh /opt/phi/infrastructure/edge/verify.sh
```

Full guide: [infrastructure/ci/README.md](infrastructure/ci/README.md), [infrastructure/edge/README.md](infrastructure/edge/README.md).

### Prod environment (`/etc/phi/env`)

| Variable | Purpose |
|----------|---------|
| `PHI_JWT_SECRET` | **Required** — session signing (min 32 chars) |
| `PHI_H2_PASSWORD` | **Required** — H2 database password |
| `CLAUDE_API_KEY` | Claude API for ingest parse |
| `PHI_DOMAIN` | Public hostname for Caddy TLS (empty = LAN HTTP) |
| `PHI_CORS_ORIGINS` | Mobile/web origins (comma-separated) |
| `PHI_RETAIN_FILES_DAYS` | `0` = purge PDF after text extract (default) |
| `PHI_CLAUDE_AUTO_PARSE` | `true` auto-parse; `false` for manual control |

## Documentation

Full technical reference: [documentation/README.md](documentation/README.md)

**Start with:** [documentation/00-engineering-principles.md](documentation/00-engineering-principles.md)

**Product direction:** one app for the whole family — equal peers, **basic mode** (simple) vs **advanced mode** (family tools + dashboards). Hybrid **H2 + DuckDB** for analytics. See [documentation/16-product-vision-family-and-modes.md](documentation/16-product-vision-family-and-modes.md).

## Phases

Track in [sprints/README.md](sprints/README.md).

| Phase | Focus | Status |
|-------|-------|--------|
| 1 | Scaffold | ✅ |
| 2 | Staged extraction + biomarkers | ✅ |
| 3a | Family platform | ✅ |
| 3b | Basic vs advanced UX | 🔶 Partial |
| 4 | Analytics (H2 + DuckDB) | ✅ |
| 4b | Data sovereignty (ephemeral PDFs, dedup) | ✅ |
| **5** | **Engineered intelligence** (rules-first, SQL insights) | **⬜ Next** |
| 6 | Polish & deploy | ⬜ |

## License

MIT — see [LICENSE](LICENSE).
