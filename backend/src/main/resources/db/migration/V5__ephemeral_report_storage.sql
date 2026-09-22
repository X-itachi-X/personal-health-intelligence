-- Ephemeral ingest artifacts: structured DB is source of truth; PDFs/images are optional short-lived inputs.

ALTER TABLE lab_reports ADD COLUMN content_hash VARCHAR(64);

ALTER TABLE lab_reports ALTER COLUMN storage_path DROP NOT NULL;

CREATE INDEX idx_lab_reports_person_content_hash ON lab_reports(person_id, content_hash);
