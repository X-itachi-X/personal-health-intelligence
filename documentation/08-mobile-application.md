# Mobile Application

The mobile app surfaces **structured biomarker data from the API** — trends, reports, family feed. It does not send PDFs to Claude; the backend stages ingest (local extract → DB → optional parse). See [00-engineering-principles.md](./00-engineering-principles.md).

**Framework:** Expo SDK 57 + React Native 0.86 + TypeScript  
**Router:** Expo Router (file-based)  
**Location:** `mobile/`

### UI design system

All screens use shared components and tokens. See:

- `mobile/DESIGN.md` — design contract (colors, typography, motion)
- `mobile/components/ui/` — `Screen`, `Card`, `Button`, `Icon`, `FadeInView`, `Skeleton`, etc.
- `mobile/lib/theme.ts` — color, spacing, typography, elevation tokens
- `.cursor/rules/mobile-ui.mdc` — Cursor rule enforced when editing `mobile/**/*.tsx`

Use Ionicons via `Icon` — no emoji for navigation or actions. Use `Skeleton` for loading, `EmptyState` for empty lists.

---

## Directory structure

```
mobile/
├── app/
│   ├── _layout.tsx           Root layout (auth gate + stack)
│   ├── login.tsx / register.tsx
│   └── (app)/                  Authenticated screens (Home, Reports, Family, Trends, Settings)
├── components/
│   ├── AppShell.tsx            Sidebar nav + header
│   ├── FamilySwitchOverlay.tsx
│   └── ui/                     Shared design system components
├── lib/
│   ├── api.ts                  Backend HTTP client
│   ├── theme.ts                Design tokens
│   ├── icons.ts                Ionicons nav map
│   └── motion.ts               Animation helpers
├── DESIGN.md                   UI design contract
├── app.json             Expo config
├── package.json         Dependencies
├── tsconfig.json
├── .env.example         Environment template
└── babel.config.js
```

---

## Screens

### Home (`app/index.tsx`)

Single screen with four sections:

| Section | Content |
|---------|---------|
| Title | "How are you doing?" |
| What is happening? | Backend connection status + parsed biomarkers |
| What should you do? | "Upload a lab report to get started" |
| Upload button | Document picker → upload → poll |

**State:**

| State | Type | Purpose |
|-------|------|---------|
| `uploading` | boolean | Disable button, show spinner |
| `status` | string \| null | Status message below button |
| `backendStatus` | `loading \| ok \| error` | Health check result |
| `biomarkers` | `Biomarker[]` | First 8 results after success |

**On mount:** `fetchHealth()` → sets backend status.

**Upload flow:**

1. `DocumentPicker.getDocumentAsync({ type: ["application/pdf", "image/*"] })`
2. `uploadReport({ uri, name, mimeType, file })`
3. `pollReportUntilDone(reportId, onUpdate)` — updates status text
4. On success: show count + first 8 biomarkers
5. On failure: show error message

---

## API client (`lib/api.ts`)

**Base URL:** `process.env.EXPO_PUBLIC_API_URL ?? "http://localhost:8080"`

### Functions

| Function | HTTP | Description |
|----------|------|-------------|
| `fetchHealth()` | `GET /api/v1/health` | Backend reachability |
| `uploadReport(file)` | `POST /api/v1/reports` | Multipart upload |
| `fetchReport(id)` | `GET /api/v1/reports/{id}` | Single report |
| `pollReportUntilDone(id, onUpdate)` | Poll `fetchReport` | Wait for terminal status |

### Upload implementation

| Platform | Method |
|----------|--------|
| Android / iOS | `XMLHttpRequest` + FormData with `{ uri, name, type }` |
| Web | `fetch` + `File` from document picker |

**Why XHR on native:** Expo SDK 57 winter fetch has FormData compatibility issues with file uploads.

### Polling defaults

| Setting | Value |
|---------|-------|
| `maxAttempts` | 90 |
| `intervalMs` | 2000 |
| Max wait | ~3 minutes |

Throws on `FAILED` status with `extractionError` message.

### TypeScript types

```typescript
type Biomarker = {
  canonical: string;
  testName: string;
  value: number | null;
  textValue: string | null;
  unit: string | null;
  referenceRange: string | null;
  confidence: number | null;
};

type ReportDetail = {
  reportId: number;
  filename: string;
  uploadedAt: string;
  extractionStatus: string;
  extractionError: string | null;
  biomarkerCount: number;
  biomarkers: Biomarker[];
};
```

---

## Configuration

### `mobile/.env`

```bash
EXPO_PUBLIC_API_URL=http://192.168.1.7:8080   # Your PC's LAN IP for phone testing
```

### `app.json`

- **Slug:** `personal-health-intelligence`
- **Platforms:** iOS, Android, Web
- **Plugins:** expo-router, expo-secure-store, expo-asset, expo-font

### npm scripts

| Script | Command |
|--------|---------|
| `npm start` | `expo start --lan` |
| `npm run start:tunnel` | `expo start --tunnel` |
| `npm run start:web` | `expo start --web` |

---

## Running on a physical phone

1. Phone and PC on same WiFi
2. Set `EXPO_PUBLIC_API_URL` to PC LAN IP (not `localhost`)
3. `npm start` → scan QR with Expo Go
4. Ensure backend CORS includes Expo origin (see [09-configuration.md](./09-configuration.md))

---

## Known gaps

| Gap | Notes |
|-----|-------|
| No report history | Only shows latest upload results |
| No out-of-range highlighting | Raw values only |
| No explanation text | Phase 3 |
| `@tanstack/react-query` | Installed but unused |
| `EXPO_PUBLIC_USE_RN_FETCH` | In `.env.example` but not used in code |
| `HealthResponse` type | Missing `claudeConfigured` fields |

---

## Future mobile features

- Report list screen
- Biomarker detail with reference range colors
- AI summary card
- Trend charts
- Push notification when extraction completes (optional)
