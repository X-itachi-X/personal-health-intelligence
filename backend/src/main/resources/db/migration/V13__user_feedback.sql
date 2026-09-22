CREATE TABLE user_feedback (
    id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL,
    submitter_email VARCHAR(320) NOT NULL,
    submitter_name VARCHAR(255) NOT NULL,
    category VARCHAR(32) NOT NULL,
    message VARCHAR(4000) NOT NULL,
    rating INTEGER,
    screen_context VARCHAR(256),
    app_platform VARCHAR(32),
    app_version VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_feedback_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_user_feedback_created ON user_feedback(created_at DESC);
CREATE INDEX idx_user_feedback_status ON user_feedback(status, created_at DESC);
