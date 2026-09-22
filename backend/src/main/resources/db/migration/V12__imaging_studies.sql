ALTER TABLE lab_reports ADD COLUMN imaging_extract CLOB;

CREATE TABLE imaging_studies (
    id                      VARCHAR(36) PRIMARY KEY,
    person_id               BIGINT NOT NULL REFERENCES persons(id),
    family_id               VARCHAR(36),
    source_report_id        BIGINT REFERENCES lab_reports(id),
    modality                VARCHAR(16) NOT NULL,
    body_region             VARCHAR(128),
    study_date              DATE NOT NULL,
    facility                VARCHAR(255),
    impression              CLOB,
    created_by_account_id   VARCHAR(36),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE imaging_findings (
    id                  VARCHAR(36) PRIMARY KEY,
    imaging_study_id    VARCHAR(36) NOT NULL REFERENCES imaging_studies(id),
    finding_text        VARCHAR(1024) NOT NULL,
    severity            VARCHAR(16),
    measurement_value   VARCHAR(64),
    measurement_unit    VARCHAR(32),
    sort_order          INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_imaging_studies_person_date ON imaging_studies(person_id, study_date DESC);
CREATE INDEX idx_imaging_findings_study ON imaging_findings(imaging_study_id);
