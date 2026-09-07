# Personal Health Intelligence

Turn uploaded lab reports into simple, decision-oriented health summaries for family members.

## Stack

| Layer | Technology |
|-------|------------|
| Mobile | Expo (React Native) + TypeScript |
| Backend | Spring Boot 4 + Java 21 |
| Database | PostgreSQL 16 |
| AI | Claude API (cloud) |
| Server | edge-server (Alpine, Proxmox) |
| Repo | [GitHub](https://github.com/X-itachi-X/personal-health-intelligence) |

## Project structure

```
personal-health-intelligence/
├── mobile/          # Expo app
├── backend/         # Spring Boot API + health engine modules
├── skills/          # Medical domain knowledge (markdown)
├── rules/           # Deterministic rules (YAML)
├── data/            # Golden test dataset
├── infrastructure/  # Caddy, deploy scripts
└── docs/            # Project notes
```

## Local development (Fedora)

### Prerequisites

- Java 21+ (you have Java 25 — works fine)
- PostgreSQL 16
- Node.js 20+ and npm (for mobile)
- `CLAUDE_API_KEY` env var (Phase 2)

### Backend

```bash
# Create DB
createdb phi
psql -c "CREATE USER phi WITH PASSWORD 'phi';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE phi TO phi;"

cd backend
./gradlew bootRun
```

Health check: `curl http://localhost:8080/api/v1/health`

### Mobile

```bash
cd mobile
npm install
EXPO_PUBLIC_API_URL=http://YOUR_LAN_IP:8080 npm start
```

## Deployment (edge-server)

1. Resize VM to 4 GiB RAM (done)
2. `doas apk add git openjdk21-jdk`
3. Add GitHub deploy key, clone to `/opt/phi`
4. Install PostgreSQL + Caddy on edge
5. Run `infrastructure/edge/deploy.sh` after each release

## Phases

1. **Setup** — scaffold, infra, golden dataset
2. **Core engineering** — extraction, rules, actions (tested independently)
3. **Integration** — full upload → analyze → explain pipeline
4. **Testing** — golden dataset regression
5. **Improvement** — UX, reminders, family accounts

## License

MIT — see [LICENSE](LICENSE).
