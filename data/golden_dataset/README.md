# Golden dataset

Regression fixtures for extraction and parsing. JSON holds expected outputs; optional source PDFs sit alongside for end-to-end checks.

## Current fixtures

| File | Type | Notes |
|------|------|-------|
| `ankit/report_2026_09.json` | Lab report | Orange Health full body panel — 106 biomarkers |
| `prescriptions/rx_diabetes_2026.json` | Prescription | Synthetic Indian Rx — Metformin, Telmisartan, Amoxicillin |
| `imaging/usg_abdomen_2026.json` | Imaging | Synthetic USG abdomen — fatty liver impression |

## Regenerate lab fixture from PDF

```bash
cd backend
set -a && source ../.env && set +a
GENERATE_GOLDEN=true ./gradlew test --tests com.phi.golden.GoldenDatasetGeneratorTest
```

Requires `CLAUDE_API_KEY`. Overwrites `ankit/report_2026_09.json`.

## Run regression tests

```bash
cd backend && ./gradlew test --tests 'com.phi.golden.*' --tests com.phi.extraction.ReportExtractionGoldenTest --tests com.phi.reasoning.yaml.CrossModalRuleEngineTest
```
