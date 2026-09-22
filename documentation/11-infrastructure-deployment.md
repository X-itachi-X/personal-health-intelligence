# Infrastructure & Deployment

Production deployment on Alpine Linux edge-server (Proxmox VM, 4 GiB RAM).

**Scripts location:** `infrastructure/edge/`

---

## Server layout

```
/var/phi/
├── phi.jar              # Deployed Spring Boot application
├── data/
│   └── phi.mv.db        # H2 database file
├── reports/             # Uploaded PDFs (immutable)
└── backups/             # Nightly backup output

/opt/phi/                # Git clone of this repository
```

---

## Scripts

| Script | Purpose |
|--------|---------|
| `setup.sh` | One-time: dirs, Java, Caddy, Tesseract, OpenRC services |
| `phi.env.example` | Template → `/etc/phi/env` (secrets, `PHI_DOMAIN`) |
| `deploy.sh` | `git pull`, build JAR, restart, health check |
| `backup.sh` | Brief stop, copy H2 + DuckDB + reports; keep 14 backups |
| `install-cron.sh` | Nightly backup at 02:00 |
| `configure-proxy.sh` | LAN HTTP or public TLS Caddyfile |
| `verify.sh` | Post-deploy smoke test (+ HTTPS) |
| `phi.openrc` | OpenRC init (sources `/etc/phi/env`) |
| `Caddyfile.lan` / `Caddyfile.tls` | Reverse proxy configs |

---

## One-time setup

```bash
# On edge-server (Alpine)
doas sh /opt/phi/infrastructure/edge/setup.sh
doas vi /etc/phi/env
sh /opt/phi/infrastructure/edge/configure-proxy.sh   # after setting PHI_DOMAIN
sh /opt/phi/infrastructure/edge/deploy.sh
sh /opt/phi/infrastructure/edge/install-cron.sh
```

### Environment on server

Copy `infrastructure/edge/phi.env.example` → `/etc/phi/env` (chmod 600).

| Variable | Value |
|----------|-------|
| `PHI_JWT_SECRET` | Required — min 32 chars |
| `PHI_H2_PASSWORD` | Required — H2 password |
| `CLAUDE_API_KEY` | Your API key |
| `PHI_DOMAIN` | Public hostname for TLS (empty = LAN HTTP) |
| `PHI_CORS_ORIGINS` | Mobile/web origins |

---

## Deploy updates

```bash
sh /opt/phi/infrastructure/edge/deploy.sh
```

**What `deploy.sh` does:**

1. `cd /opt/phi && git pull origin main`
2. `cd backend && ./gradlew bootJar -x test`
3. Copy JAR to `/var/phi/phi.jar`
4. `doas rc-service phi restart`

**Note:** Tests are skipped on deploy (`-x test`).

---

## OpenRC service (`phi.openrc`)

| Setting | Value |
|---------|-------|
| Command | `java -jar /var/phi/phi.jar` |
| Profile | `SPRING_PROFILES_ACTIVE=prod` |
| Log file | `/var/log/phi.log` |
| User | `dev` (adjust as needed) |

---

## Caddy reverse proxy

| Mode | File | When |
|------|------|------|
| LAN HTTP | `Caddyfile.lan` | `PHI_DOMAIN` unset |
| Public HTTPS | `Caddyfile.tls` | `PHI_DOMAIN=phi.example.com` |

Run `configure-proxy.sh` after editing `/etc/phi/env`. Caddy obtains Let's Encrypt certificates automatically when a domain is set.

---

## Backup

```bash
sh /opt/phi/infrastructure/edge/install-cron.sh
```

**`backup.sh`:**

1. Stop `phi` service briefly for consistent H2 copy
2. Copy `phi.mv.db`, `analytics.duckdb`, and `reports/` tarball to `/var/phi/backups/{timestamp}/`
3. Write `manifest.txt` with byte sizes
4. Delete backups older than 14 days

---

## Resource usage

| Component | RAM (approx.) |
|-----------|---------------|
| Spring Boot + H2 | 512–768 MiB |
| Caddy | ~50 MiB |
| Alpine OS | ~300 MiB |
| **Total** | ~1 GiB |

Comfortable on 4 GiB VM with headroom.

---

## Network diagram (production)

```
Internet / LAN
      │
      ▼
  Caddy :80 (or :443)
      │
      ▼
Spring Boot :8080
      ├── H2 /var/phi/data/phi.mv.db
      ├── PDFs /var/phi/reports/
      └── Claude API (outbound HTTPS)
```

---

## Dev vs prod comparison

| | Dev (Fedora) | Prod (Alpine edge) |
|--|--------------|-------------------|
| Start | `./backend/run-dev.sh` | `rc-service phi start` |
| Profile | `dev` | `prod` |
| H2 path | `~/.phi/h2db` | `/var/phi/data/phi` |
| H2 console | Enabled | Disabled |
| Proxy | None | Caddy |
| Secrets | `.env` file | Server environment |

---

## Troubleshooting

| Issue | Check |
|-------|-------|
| Service won't start | `tail /var/log/phi.log` |
| H2 lock error | Kill stale Java process before restart |
| Mobile can't connect | Firewall, CORS origins, correct server IP |
| Deploy fails | Java version, git access, disk space |
