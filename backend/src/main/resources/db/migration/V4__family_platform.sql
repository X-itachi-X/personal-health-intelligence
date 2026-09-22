-- Phase 3a: accounts, multi-family memberships, invites, audit

ALTER TABLE persons ADD COLUMN date_of_birth DATE;
ALTER TABLE persons ADD COLUMN sex VARCHAR(16);
ALTER TABLE persons ADD COLUMN height_cm DECIMAL(5, 1);
ALTER TABLE persons ADD COLUMN weight_kg DECIMAL(5, 1);
ALTER TABLE persons ADD COLUMN blood_group VARCHAR(8);
ALTER TABLE persons ADD COLUMN medications VARCHAR(1024);
ALTER TABLE persons ADD COLUMN conditions VARCHAR(1024);
ALTER TABLE persons ADD COLUMN notes VARCHAR(1024);

CREATE TABLE accounts (
    id              VARCHAR(36) PRIMARY KEY,
    person_id       BIGINT NOT NULL UNIQUE REFERENCES persons(id),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    device_id       VARCHAR(255),
    ui_mode         VARCHAR(16) NOT NULL DEFAULT 'basic',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at   TIMESTAMP
);

CREATE TABLE families (
    id                    VARCHAR(36) PRIMARY KEY,
    display_name          VARCHAR(255) NOT NULL,
    created_by_account_id VARCHAR(36) NOT NULL UNIQUE REFERENCES accounts(id),
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE family_memberships (
    id           VARCHAR(36) PRIMARY KEY,
    family_id    VARCHAR(36) NOT NULL REFERENCES families(id),
    person_id    BIGINT NOT NULL REFERENCES persons(id),
    relationship VARCHAR(32) NOT NULL,
    family_role  VARCHAR(16) NOT NULL DEFAULT 'member',
    joined_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at      TIMESTAMP,
    UNIQUE (family_id, person_id)
);

CREATE INDEX idx_family_memberships_person ON family_memberships(person_id);
CREATE INDEX idx_family_memberships_family ON family_memberships(family_id);

CREATE TABLE invites (
    id           VARCHAR(36) PRIMARY KEY,
    family_id    VARCHAR(36) NOT NULL REFERENCES families(id),
    person_id    BIGINT NOT NULL REFERENCES persons(id),
    code         VARCHAR(8) NOT NULL UNIQUE,
    created_by   VARCHAR(36) NOT NULL REFERENCES accounts(id),
    expires_at   TIMESTAMP NOT NULL,
    accepted_at  TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_events (
    id               VARCHAR(36) PRIMARY KEY,
    family_id        VARCHAR(36) REFERENCES families(id),
    actor_account_id VARCHAR(36) REFERENCES accounts(id),
    action           VARCHAR(64) NOT NULL,
    target_type      VARCHAR(32),
    target_id        VARCHAR(64),
    metadata         VARCHAR(2048),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE lab_reports ADD COLUMN family_id VARCHAR(36);
ALTER TABLE lab_reports ADD COLUMN uploaded_by_account_id VARCHAR(36);
ALTER TABLE lab_reports ADD COLUMN deleted_at TIMESTAMP;

CREATE INDEX idx_lab_reports_family ON lab_reports(family_id);
