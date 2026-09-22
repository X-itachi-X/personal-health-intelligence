# Local Development Guide

Step-by-step setup on Fedora (or similar Linux).

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java | 21+ | `java-25-openjdk-devel` |
| Node.js | 22+ | `nodejs npm` |
| Git | any | `git` |

---

## 1. Clone and configure secrets

```bash
git clone https://github.com/X-itachi-X/personal-health-intelligence.git
cd personal-health-intelligence
cp .env.example .env
```

Edit `.env`:

```bash
CLAUDE_API_KEY=sk-ant-...
CLAUDE_WORKSPACE_ID=wrkspc_...
PHI_CORS_ORIGINS=http://localhost:8081,http://127.0.0.1:8081,http://YOUR_LAN_IP:8081
```

Get API key: [platform.claude.com](https://platform.claude.com) → Settings → API Keys.

---

## 2. Start backend

```bash
cd backend
./run-dev.sh
```

**What it does:** Sources `../.env`, runs `./gradlew bootRun` with `dev` profile.

**Verify:**

```bash
curl http://localhost:8080/api/v1/health
# Expect: claudeConfigured: true
```

**H2 console:** http://localhost:8080/h2-console  
JDBC URL: `jdbc:h2:file:~/.phi/h2db` | User: `sa` | Password: (empty)

**Data locations:**

| Resource | Path |
|----------|------|
| Database | `~/.phi/h2db.mv.db` |
| PDFs | `~/.phi/reports/` |

---

## 3. Start mobile app

```bash
cd mobile
cp .env.example .env
# Edit EXPO_PUBLIC_API_URL to your PC's LAN IP for phone testing
npm install
npm start
```

| Target | Action |
|--------|--------|
| Phone (Expo Go) | Scan QR code — use LAN URL |
| Web | Press `w` in terminal |
| Android emulator | Press `a` |

**Phone testing:** Set `EXPO_PUBLIC_API_URL=http://192.168.x.x:8080` (not `localhost`).

---

## 4. Test upload

### Via curl

```bash
curl -X POST http://localhost:8080/api/v1/reports \
  -F "file=@/path/to/lab-report.pdf"
```

Poll:

```bash
curl http://localhost:8080/api/v1/reports/1
```

### Via mobile

1. Tap "Upload report"
2. Select PDF
3. Wait 2–3 minutes (large panels)
4. See biomarker count + first 8 results

---

## 5. Run tests

```bash
cd backend
./gradlew test
```

---

## Common issues

| Problem | Fix |
|---------|-----|
| `claudeConfigured: false` | Set `CLAUDE_API_KEY` in `.env`, restart backend |
| Workspace header error | Add `CLAUDE_WORKSPACE_ID` to `.env` |
| H2 database locked | Kill old Java process: `pkill -f PhiApplication` |
| Phone can't reach API | Use LAN IP in `EXPO_PUBLIC_API_URL`, check firewall |
| CORS error | Add Expo origin to `PHI_CORS_ORIGINS` |
| Upload FormData error | Ensure latest `mobile/lib/api.ts` (XHR on Android) |
| Extraction timeout on phone | Wait up to 3 min; check backend logs |
| Expo blank logs | Normal until phone connects |

---

## Backend logs

Watch extraction in terminal running `run-dev.sh`:

```
INFO  Report 10 extraction completed with 131 biomarkers
ERROR Extraction failed for report X
```

Async thread names: `phi-extract-1`, `phi-extract-2`, etc.

---

## Stopping services

| Service | How |
|---------|-----|
| Backend | Ctrl+C in terminal, or `pkill -f bootRun` |
| Expo | Ctrl+C in mobile terminal |

---

## IDE tips

- Open `backend/` as Java project for Gradle import
- Open `mobile/` for TypeScript/Expo
- Full docs: [documentation/README.md](./README.md)
