# Village Labs, Scans & Handwritten Reports

**Primary users:** elderly family members in villages and small towns — paper reports, phone scans (CamScanner, Adobe Scan), photos.

_Last updated: 2026-09-18_

---

## Reality on the ground

| Report type | Common? | Our target |
|-------------|---------|------------|
| Fancy digital PDF (Orange Health, etc.) | Urban, younger | Supported well |
| **Printed slip from local lab** | Very common | **OCR → parse** |
| **Scanned PDF (scanner apps)** | Very common | **OCR → parse** |
| **Phone photo of report** | Very common | **OCR → parse** |
| **Handwritten values** | Common in villages | **Hard — partial support, review needed** |

PHI must work for the middle three. Handwriting is the hardest problem in the industry — we are honest about limits.

---

## Pipeline (AI is always last)

```
Upload (PDF / image)
    → 1. PDF text layer (digital PDFs)
    → 2. Local OCR (Tesseract) — scans, photos, village slips
    → 3. Claude vision — ONLY if 1–2 return blank (opt-in: PHI_CLAUDE_VISION_FALLBACK)
    → Save extracted_text to H2 → delete file
    → 4. Rules/templates (if format matches)
    → 5. Claude text parse — ONLY if rules coverage too low
```

**Claude never runs first.** Local methods are free, private, and fast. AI is the last resort.

---

## Implementation status

| Capability | Status |
|------------|--------|
| Detect thin/missing PDF text layer | **Done** |
| OCR scanned PDF (render pages → Tesseract) | **Done** |
| OCR image uploads (JPEG, PNG, …) | **Done** |
| Health check `ocrAvailable` | **Done** |
| Orange Health rules (digital PDFs) | **Done** |
| Generic village lab templates | **Planned** |
| Handwriting-optimized OCR / review UI | **Planned** |
| Hindi/regional language packs | **Config** (`PHI_OCR_LANGUAGES=eng+hin`) |

---

## Server setup (required for scans)

Install Tesseract on the backend host:

```bash
# Fedora
sudo dnf install tesseract tesseract-langpack-eng tesseract-langpack-hin

# Verify
curl http://localhost:8080/api/v1/health
# expect: "ocrAvailable": true
```

Environment:

```bash
PHI_OCR_ENABLED=true
PHI_OCR_LANGUAGES=eng+hin
# optional if non-standard path:
PHI_TESSDATA_PATH=/usr/share/tesseract/tessdata
```

---

## Reliability expectations (honest)

### Printed local lab slip (clear scan)

- **Good lighting, flat photo, printed text** → OCR usually works → Claude parses values from text
- User should hold phone steady, avoid shadows

### Scanned PDF (CamScanner / Adobe Scan)

- **Reliable** if scan quality is good — same OCR path as images embedded in PDF

### Handwritten reports

- **Not fully reliable today.** Tesseract is built for printed text.
- Messy handwriting → OCR errors → wrong or missing biomarkers
- **Roadmap:** family member review screen, edit values before save, optional vision assist only when OCR confidence is very low

We do **not** claim 100% accuracy on handwriting. We aim for **useful capture + human review** for elders.

---

## Product guidance for families

1. **Best:** ask lab for printed slip + take clear photo in good light  
2. **OK:** CamScanner / Adobe Scan PDF upload  
3. **Hard:** doctor's handwritten notebook — upload anyway, but **verify results** with family  
4. Advanced mode: re-upload with better scan if extraction failed

---

## Engineering principles (unchanged)

- DB is source of truth — not the photo  
- OCR is Phase 1 (local, free)  
- Claude only on text in H2, sparingly  
- Future chatbot queries biomarker rows, not photos  

See [00-engineering-principles.md](./00-engineering-principles.md).
