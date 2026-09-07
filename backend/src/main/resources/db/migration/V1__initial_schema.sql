-- Core schema placeholder for Phase 1. Expanded in Phase 2.

CREATE TABLE persons (
    id          BIGSERIAL PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE lab_reports (
    id              BIGSERIAL PRIMARY KEY,
    person_id       BIGINT NOT NULL REFERENCES persons(id),
    original_filename VARCHAR(512) NOT NULL,
    storage_path    VARCHAR(1024) NOT NULL,
    report_date     DATE,
    lab_name        VARCHAR(255),
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lab_reports_person_id ON lab_reports(person_id);
