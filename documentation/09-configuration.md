# Configuration

All configuration for PHI across environments.

---

## Environment variables

### Root `.env` (backend — gitignored)

Copy from `.env.example`:

```bash
CLAUDE_API_KEY=sk-ant-...
CLAUDE_WORKSPACE_ID=wrkspc_...
PHI_H2_PASSWORD=          # prod H2 password (optional in dev)
PHI_CORS_ORIGINS=http://localhost:8081,http://127.0.0.1:8081,http://192.168.1.7:8081
PHI_RETAIN_FILES_DAYS=0   # delete PDF after local text saved (default)
PHI_CLAUDE_AUTO_PARSE=true  # false in dev to save tokens; manual POST .../parse
```

| Variable | Used by | Description |
|----------|---------|-------------|
| `CLAUDE_API_KEY` | Backend | Anthropic API — **Phase 2 ingest only** |
| `CLAUDE_WORKSPACE_ID` | Backend | `anthropic-workspace-id` header |
| `CLAUDE_MODEL` | Backend | Override model (default `claude-sonnet-4-6`) |
| `PHI_CLAUDE_AUTO_PARSE` | Backend | `true` = auto-run parse after local extract; `false` = manual |
| `PHI_CLAUDE_MIN_COVERAGE` | Backend | Rules-first gate (default `0.8`) — skip Claude when coverage met |
| `PHI_RETAIN_FILES_DAYS` | Backend | `0` = purge PDF after Phase 1; `N` = keep N days |
| `PHI_H2_PASSWORD` | Backend prod | H2 database password |
| `PHI_CORS_ORIGINS` | Backend prod | Comma-separated allowed origins |
| `SPRING_PROFILES_ACTIVE` | Backend | `dev` (default) or `prod` |

**Token discipline:** Use `PHI_CLAUDE_AUTO_PARSE=false` during UI development. Uploads still save `extracted_text` to H2. Trigger parse with `POST /api/v1/reports/{id}/parse`.

**Loaded by:** `backend/run-dev.sh` sources `../.env` before `bootRun`.

### Mobile `mobile/.env` (gitignored)

```bash
EXPO_PUBLIC_API_URL=http://192.168.1.7:8080
```

Only `EXPO_PUBLIC_*` vars are exposed to the Expo bundle.

---

## Spring profiles

Activated via `SPRING_PROFILES_ACTIVE` (default: `dev`).

### Base — `application.yml`

| Setting | Value |
|---------|-------|
| Server port | 8080 |
| Multipart max | 25 MB |
| JPA `ddl-auto` | `validate` |
| Flyway | enabled |
| Reports dir (default) | `${user.home}/.phi/reports` |
| Claude model | `${CLAUDE_MODEL:claude-sonnet-4-6}` |
| CORS (default) | `localhost:8081`, `127.0.0.1:8081` |
| Actuator | `health`, `info` exposed |
| Logging | `com.phi: INFO` |

### Dev — `application-dev.yml`

| Setting | Value |
|---------|-------|
| H2 URL | `jdbc:h2:file:${user.home}/.phi/h2db` |
| H2 user | `sa` (no password) |
| H2 console | enabled at `/h2-console` |
| Logging | `com.phi: DEBUG` |

### Prod — `application-prod.yml`

| Setting | Value |
|---------|-------|
| H2 URL | `jdbc:h2:file:/var/phi/data/phi` |
| H2 user | `phi` |
| H2 password | `${PHI_H2_PASSWORD}` |
| H2 console | disabled |
| Reports dir | `/var/phi/reports` |
| CORS | `${PHI_CORS_ORIGINS}` |

### Test — `backend/src/test/resources/application.yml`

| Setting | Value |
|---------|-------|
| H2 | in-memory |
| Flyway | disabled |
| JPA | `create-drop` |

---

## PhiProperties binding

```yaml
phi:
  storage:
    reports-dir: ...
  claude:
    api-key: ${CLAUDE_API_KEY:}
    workspace-id: ${CLAUDE_WORKSPACE_ID:}
    model: ${CLAUDE_MODEL:claude-sonnet-4-6}
  cors:
    allowed-origins: ...
```

Java record: `com.phi.config.PhiProperties`

---

## Async thread pool

`AsyncConfig`:

| Setting | Value |
|---------|-------|
| Core pool size | 2 |
| Max pool size | 4 |
| Thread prefix | `phi-extract-` |

---

## Claude client tuning

Hardcoded in `ClaudeExtractionClient`:

| Setting | Value |
|---------|-------|
| API URL | `https://api.anthropic.com/v1/messages` |
| Connect timeout | 30 seconds |
| Request timeout | 10 minutes |
| Max tokens | 16384 |
| Input text limit | 50,000 characters |

---

## File paths summary

| Resource | Dev | Prod |
|----------|-----|------|
| H2 database | `~/.phi/h2db.mv.db` | `/var/phi/data/phi.mv.db` |
| PDF reports | `~/.phi/reports/` | `/var/phi/reports/` |
| Application JAR | `backend/build/libs/` | `/var/phi/phi.jar` |
| Logs (prod) | console | `/var/log/phi.log` |
| Backups | — | `/var/phi/backups/` |

---

## CORS setup for phone testing

Add your Expo dev server origin to `PHI_CORS_ORIGINS`:

```
http://192.168.1.7:8081    # Expo LAN
http://localhost:8081      # Expo web
```

Restart backend after changing `.env`.
