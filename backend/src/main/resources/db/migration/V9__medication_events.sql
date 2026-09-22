CREATE TABLE medication_events (
    id                      VARCHAR(36) PRIMARY KEY,
    person_id               BIGINT NOT NULL REFERENCES persons(id),
    family_id               VARCHAR(36),
    medication_name         VARCHAR(255) NOT NULL,
    dosage                  VARCHAR(128),
    started_on              DATE,
    ended_on                DATE,
    notes                   VARCHAR(1024),
    created_by_account_id   VARCHAR(36),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_medication_events_person ON medication_events(person_id);
CREATE INDEX idx_medication_events_family ON medication_events(family_id);
