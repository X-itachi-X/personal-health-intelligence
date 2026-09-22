# Security

Current security posture and known gaps.

---

## Threat model (family app)

| Concern | Current approach |
|---------|------------------|
| Data privacy | Self-hosted option; PDFs on local disk |
| API access | Open on LAN (dev) — no auth yet |
| Secrets | `.env` gitignored; Claude key server-side only |
| Transport | HTTP on LAN; HTTPS via Caddy in prod (when configured) |

---

## Spring Security configuration

**File:** `backend/src/main/java/com/phi/config/SecurityConfig.java`

| Setting | Value |
|---------|-------|
| CSRF | Disabled |
| Session | `STATELESS` |
| Auth provider | **None configured** |

### Public paths (no authentication)

| Path | Purpose |
|------|---------|
| `/actuator/health` | Health probe |
| `/api/v1/health` | App health |
| `/api/v1/reports/**` | Upload + query (all methods) |
| `/h2-console/**` | H2 admin UI (dev only) |

### Protected paths

Everything else requires `.authenticated()` — but **no login mechanism exists**, so unlisted paths are effectively blocked.

---

## CORS

**File:** `backend/src/main/java/com/phi/config/WebConfig.java`

| Setting | Value |
|---------|-------|
| Paths | `/api/**` |
| Origins | `phi.cors.allowed-origins` (comma-split) |
| Methods | GET, POST, PUT, DELETE, OPTIONS |
| Headers | All (`*`) |

---

## Secrets handling

| Secret | Where stored | Never in |
|--------|--------------|----------|
| `CLAUDE_API_KEY` | Root `.env` | Mobile app, git, `.env.example` |
| `CLAUDE_WORKSPACE_ID` | Root `.env` | Mobile app, git |
| `PHI_H2_PASSWORD` | Server env (prod) | Git |

Mobile app only knows `EXPO_PUBLIC_API_URL` — no AI keys on device.

---

## Data flow security

```
Phone ──HTTP──► Backend ──HTTPS──► Claude API
                    │
                    ├── PDF never sent to Claude
                    └── Only extracted text sent to Claude
```

---

## H2 console (dev risk)

In dev profile, H2 console is **publicly accessible** (no auth) at `/h2-console`.

- Full read/write access to database
- **Disable in prod** (already disabled in `application-prod.yml`)
- Do not expose port 8080 to the public internet without auth

---

## Production recommendations (not yet implemented)

| Priority | Measure |
|----------|---------|
| High | Add API authentication (JWT or API key per device) |
| High | TLS via Caddy with real domain + Let's Encrypt |
| High | Firewall: only allow LAN or VPN to port 8080/443 |
| Medium | Rate limiting on upload endpoint |
| Medium | H2 password in prod (`PHI_H2_PASSWORD`) |
| Medium | Global exception handler (no stack traces to clients) |
| Low | Audit log for report access |

---

## Dependency security

- Spring Boot 4 / Spring Security 7 manage HTTP security defaults
- PDFBox processes untrusted PDFs locally — standard PDF parsing risks apply
- Claude API: data sent to Anthropic cloud per their privacy policy

---

## `.gitignore` protections

```
.env
.env.*
!.env.example
mobile/.env
mobile/.env.local
```

Never commit real API keys. If leaked, rotate immediately in Anthropic console.
