# Edge server (Alpine)

Single-process deployment: Spring Boot + embedded H2 + DuckDB analytics + ephemeral PDF storage.

## Layout on edge-server

```
/var/phi/
├── phi.jar              # deployed Spring Boot app
├── data/
│   ├── phi.mv.db        # H2 database file
│   └── analytics.duckdb # DuckDB analytics (synced from H2)
├── reports/             # uploaded files (ephemeral when PHI_RETAIN_FILES_DAYS=0)
└── backups/             # nightly backup output (14 retained)

/etc/phi/env             # production secrets (chmod 600)
```

## Scripts

| Script | Purpose |
|--------|---------|
| `setup.sh` | One-time: dirs, Java, Caddy, Tesseract, OpenRC services |
| `phi.env.example` | Template for `/etc/phi/env` |
| `deploy.sh` | `git pull`, build JAR, restart, health check |
| `backup.sh` | Stop service briefly, copy H2 + DuckDB + reports |
| `install-cron.sh` | Nightly backup at 02:00 |
| `configure-proxy.sh` | LAN HTTP or public TLS Caddyfile |
| `verify.sh` | Post-deploy smoke test (+ HTTPS when `PHI_DOMAIN` set) |
| `phi.openrc` | OpenRC service (sources `/etc/phi/env`) |
| `Caddyfile.lan` | HTTP reverse proxy on `:80` |
| `Caddyfile.tls` | Let's Encrypt HTTPS template |

## Quick start

```bash
# On edge-server (Alpine) after cloning to /opt/phi
doas sh /opt/phi/infrastructure/edge/setup.sh
doas vi /etc/phi/env          # JWT secret, H2 password, Claude key, CORS

# Optional public HTTPS
# Set PHI_DOMAIN=phi.yourdomain.com in /etc/phi/env
sh /opt/phi/infrastructure/edge/configure-proxy.sh

sh /opt/phi/infrastructure/edge/deploy.sh
sh /opt/phi/infrastructure/edge/install-cron.sh
sh /opt/phi/infrastructure/edge/verify.sh
```

## TLS

1. Point DNS `A` record at the edge server.
2. Open ports **80** and **443** on the firewall.
3. Set `PHI_DOMAIN` in `/etc/phi/env`.
4. Run `configure-proxy.sh` — Caddy obtains a Let's Encrypt certificate automatically.

Leave `PHI_DOMAIN` empty for LAN-only HTTP (`Caddyfile.lan`).

## Backups

`backup.sh` stops the API for ~5 seconds, copies:

- `phi.mv.db` (+ trace file if present)
- `analytics.duckdb`
- `reports/` tarball

Keeps the **14 most recent** backup folders under `/var/phi/backups/`.

**Restore (manual):**

```bash
doas rc-service phi stop
doas cp /var/phi/backups/YYYYMMDD_HHMMSS/phi.mv.db /var/phi/data/phi.mv.db
# optional: analytics.duckdb, reports.tar.gz
doas rc-service phi start
```

## Resource usage (approx.)

| Component | RAM |
|-----------|-----|
| Spring Boot + H2 embedded | ~512–768 MiB |
| Caddy | ~50 MiB |
| Alpine OS | ~300 MiB |
| **Total** | ~1 GiB |
