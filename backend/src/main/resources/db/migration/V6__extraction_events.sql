-- Pipeline telemetry: per-step audit trail for extractions (text, rules, AI, errors)

CREATE TABLE extraction_events (
    id               VARCHAR(36) PRIMARY KEY,
    lab_report_id    BIGINT NOT NULL REFERENCES lab_reports(id),
    family_id        VARCHAR(36),
    event_type       VARCHAR(64) NOT NULL,
    status           VARCHAR(16) NOT NULL,
    duration_ms      BIGINT,
    char_count       INTEGER,
    biomarker_count  INTEGER,
    coverage         DOUBLE,
    input_tokens     INTEGER,
    output_tokens    INTEGER,
    model            VARCHAR(64),
    message          VARCHAR(512),
    metadata         VARCHAR(2048),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_extraction_events_report ON extraction_events(lab_report_id);
CREATE INDEX idx_extraction_events_created ON extraction_events(created_at DESC);
CREATE INDEX idx_extraction_events_type ON extraction_events(event_type);
CREATE INDEX idx_extraction_events_family ON extraction_events(family_id);

CREATE INDEX idx_audit_events_family_created ON audit_events(family_id, created_at DESC);
