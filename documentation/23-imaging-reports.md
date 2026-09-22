# Imaging reports (USG, X-ray, MRI, CT)

Structured memory from radiology **report text** — not DICOM pixel storage.

_Last updated: 2026-09-19_

---

## Why imaging matters

Labs show **numbers**. Imaging shows **what is there** — fatty liver, kidney cysts, cardiomegaly. Together they give families a clearer picture than blood tests alone.

PHI stores the **impression and findings** from the printed/PDF report, then correlates with labs and medications over time.

---

## Ingest flow

```
Upload scan report (PDF/photo)
  → Phase 1: PDF text / OCR
  → Phase 2: ImagingTextParser (+ Claude once if needed)
  → User confirms study + findings
  → imaging_studies + imaging_findings tables
  → Purge file
```

`document_type = IMAGING_REPORT` on `lab_reports`.

---

## Schema

| Table | Purpose |
|-------|---------|
| `imaging_studies` | Modality, body region, study date, impression |
| `imaging_findings` | Individual finding lines with optional severity |

Pending extract JSON stored in `lab_reports.imaging_extract` until confirmed.

---

## APIs

| Endpoint | Purpose |
|----------|---------|
| `POST /api/v1/reports?documentType=IMAGING_REPORT` | Upload |
| `GET /api/v1/reports/{id}/imaging-draft` | Parsed draft |
| `POST /api/v1/reports/{id}/confirm-imaging` | Save to timeline |
| `GET /api/v1/persons/{id}/imaging-studies` | Person timeline |
| Agent: `/api/v1/agent/tools/persons/{id}/imaging-studies` | Chat foundation |

---

## Cross-modal insights

`ImagingCorrelationService` links imaging text with latest lab findings, e.g.:

- Fatty liver on USG + elevated ALT
- Kidney findings + creatinine context

Home insight cards use template text — no per-question Claude.

---

## Out of scope

- DICOM storage / PACS viewer
- AI on scan pixels
- Diagnosis or treatment advice

---

## Related

- [22-medication-and-prescriptions.md](./22-medication-and-prescriptions.md)
- [00-engineering-principles.md](./00-engineering-principles.md)
