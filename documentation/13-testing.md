# Testing

Current test coverage and gaps.

---

## Backend tests

**Location:** `backend/src/test/java/`

| Test class | What it covers |
|------------|----------------|
| `PhiApplicationTests` | Spring context loads (`@SpringBootTest`) |
| `BiomarkerNormalizerTest` | 4 alias normalizations |
| `ClaudeResponseParserTest` | Plain JSON + markdown-fenced JSON parsing; `max_tokens` detection |
| `GoldenDatasetRegressionTest` | Loads and validates all `data/golden_dataset/**/report_*.json` fixtures |
| `ReportExtractionGoldenTest` | Persists golden biomarkers via `completeExtraction` and asserts values match |
| `ReportRetryTest` | Retry rejects `PROCESSING`, accepts `FAILED` |

**Run:**

```bash
cd backend
./gradlew test
```

### Test configuration

`backend/src/test/resources/application.yml`:

- In-memory H2 (`jdbc:h2:mem:phi`)
- Flyway **disabled**
- JPA `ddl-auto: create-drop`

---

## What is NOT tested

| Area | Risk |
|------|------|
| `ReportController` / `ReportQueryController` | HTTP contract untested |
| `ReportIngestionService` | File save logic untested |
| `ReportExtractionService` | Full pipeline untested |
| `ClaudeExtractionClient` | Live API calls not mocked |
| `PdfTextExtractor` | Real PDF parsing untested |
| Flyway migrations | Schema drift undetected |
| Security / CORS | Open endpoints unverified |
| Golden `expected_findings` rules | Rule engine not wired — biomarker values only |

---

## Mobile tests

**No test files** in `mobile/`.

`@tanstack/react-query` is in `package.json` but unused — candidate for future API caching tests.

---

## Deploy testing

`infrastructure/edge/deploy.sh` runs:

```bash
./gradlew bootJar -x test
```

Tests are **skipped on production deploy**.

---

## Recommended test plan (Phase 4)

### Priority 1 — Extraction regression

```java
@Test
void parsesGoldenDatasetBiomarkers() {
    // Load report_2026_09.json
    // Compare canonical names and values within tolerance
}
```

### Priority 2 — API integration

```java
@WebMvcTest(ReportController.class)
void uploadReturnsPendingStatus() { ... }
```

### Priority 3 — PDF extraction

```java
@Test
void extractsTextFromSamplePdf() {
    String text = extractor.extractText(Path.of("testdata/sample.pdf"));
    assertThat(text).contains("Hemoglobin");
}
```

### Priority 4 — Claude client (mocked)

```java
@Test
void parsesMockClaudeResponse() {
    // WireMock or MockWebServer for Messages API
}
```

### Priority 5 — Mobile

- Jest unit tests for `lib/api.ts` poll logic
- Detox/E2E for upload flow (optional)

---

## Manual verification checklist

| Check | Command / action |
|-------|------------------|
| Backend health | `curl http://localhost:8080/api/v1/health` |
| Upload PDF | `curl -F file=@report.pdf http://localhost:8080/api/v1/reports` |
| Poll report | `curl http://localhost:8080/api/v1/reports/{id}` |
| Mobile upload | Upload from Expo Go, wait ~3 min |
| H2 data | Dev console → query `biomarker_values` |

---

## CI (not configured)

No GitHub Actions workflow exists yet. Recommended:

```yaml
# .github/workflows/backend.yml
- run: cd backend && ./gradlew test
```
