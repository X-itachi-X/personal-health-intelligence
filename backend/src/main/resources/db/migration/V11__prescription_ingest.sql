ALTER TABLE lab_reports ADD COLUMN document_type VARCHAR(16) NOT NULL DEFAULT 'LAB_REPORT';
ALTER TABLE lab_reports ADD COLUMN prescription_extract CLOB;

CREATE INDEX idx_lab_reports_document_type ON lab_reports(person_id, document_type);
