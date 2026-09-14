# Personal Health Intelligence

Turn uploaded lab reports into simple, decision-oriented health summaries for family members.

## Stack

| Layer | Technology |
|-------|------------|
| Mobile | Expo (React Native) + TypeScript |
| Backend | Spring Boot 4 + Java 21 |
| Database | **H2** (embedded file DB — dev + prod) |
| AI | Claude API (cloud) |
| Server | edge-server (Alpine, Proxmox, 4 GiB RAM) |
| Repo | [GitHub](https://github.com/X-itachi-X/personal-health-intelligence) |

## Why H2 everywhere?

Family-scale traffic, single Spring Boot process, minimal RAM on edge-server. No separate database daemon.

| Location | H2 file | Reports |
|----------|---------|---------|
| **Dev** (Fedora) | `~/.phi/h2db.mv.db` | `~/.phi/reports/` |
| **Prod** (edge) | `/var/phi/data/phi.mv.db` | `/var/phi/reports/` |

## Project structure

```
personal-health-intelligence/
├── mobile/          # Expo app
├── backend/         # Spring Boot API + health engine modules
├── skills/          # Medical domain knowledge (markdown)
├── rules/           # Deterministic rules (YAML)
├── data/            # Golden test dataset
├── infrastructure/  # Caddy, deploy, backup scripts
└── docs/            # Project notes
```

## Local development (Fedora)

### Prerequisites

- Java 21+ (`java-25-openjdk-devel`)
- Node.js 22+ and npm (for mobile)
- Claude API key (see below)

### Claude API setup

1. Sign up at [platform.claude.com](https://platform.claude.com) (Google or email).
2. Add billing: **Settings → Billing** — add a card and buy credits (~$5 is plenty to start). API keys do not work until billing is funded.
3. Create a key: **Settings → API Keys → Create Key**. Copy it immediately (`sk-ant-...`); it is shown only once.
4. Paste into project root `.env`:
   ```bash
   cp .env.example .env
   # edit CLAUDE_API_KEY=sk-ant-...
   ```
5. Start the backend with `./backend/run-dev.sh` (loads `.env` automatically).
6. Verify: `curl http://localhost:8080/api/v1/health` should show `"claudeConfigured": true`.

**Cost (family use):** ~₹5–7 per lab report with Sonnet; switch to Haiku in `application.yml` for ~₹2.5/report.

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

No PostgreSQL required — one JVM runs API + database.

```bash
# One-time setup on edge
doas sh /opt/phi/infrastructure/edge/setup.sh
doas cp /opt/phi/infrastructure/edge/phi.openrc /etc/init.d/phi
doas rc-update add phi default

# Deploy updates
sh /opt/phi/infrastructure/edge/deploy.sh

# Nightly backup (add to crontab)
0 2 * * * /opt/phi/infrastructure/edge/backup.sh
```

### Prod environment variables

| Variable | Purpose |
|----------|---------|
| `SPRING_PROFILES_ACTIVE` | Set to `prod` (done in openrc) |
| `CLAUDE_API_KEY` | Claude API for extraction |
| `PHI_H2_PASSWORD` | Optional H2 password |
| `PHI_CORS_ORIGINS` | Mobile/web origins (comma-separated) |

## Phases

1. **Setup** — scaffold, infra, golden dataset
2. **Core engineering** — extraction, rules, actions (tested independently)
3. **Integration** — full upload → analyze → explain pipeline
4. **Testing** — golden dataset regression
5. **Improvement** — UX, reminders, family accounts

## License

MIT — see [LICENSE](LICENSE).
