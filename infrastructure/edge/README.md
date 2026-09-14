# Edge server (Alpine)

Single-process deployment: Spring Boot + embedded H2 + PDF storage.

## Layout on edge-server

```
/var/phi/
├── phi.jar              # deployed Spring Boot app
├── data/
│   └── phi.mv.db        # H2 database file
├── reports/             # immutable uploaded PDFs
└── backups/             # nightly backup output

/opt/phi/                # git clone of this repo
```

## Scripts

| Script | Purpose |
|--------|---------|
| `setup.sh` | One-time: dirs, `openjdk21-jre`, git |
| `deploy.sh` | `git pull`, build JAR, restart service |
| `backup.sh` | Copy H2 file + reports tarball |
| `phi.openrc` | OpenRC service definition |
| `Caddyfile` | Reverse proxy to `:8080` |

## Resource usage (approx.)

| Component | RAM |
|-----------|-----|
| Spring Boot + H2 embedded | ~512–768 MiB |
| Caddy | ~50 MiB |
| Alpine OS | ~300 MiB |
| **Total** | ~1 GiB (comfortable on 4 GiB VM) |
