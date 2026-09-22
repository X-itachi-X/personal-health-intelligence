ALTER TABLE medication_events ADD COLUMN course_type VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE medication_events ADD COLUMN schedule_text VARCHAR(255);
ALTER TABLE medication_events ADD COLUMN duration_days INT;
ALTER TABLE medication_events ADD COLUMN expected_end_on DATE;
ALTER TABLE medication_events ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE medication_events ADD COLUMN source_report_id BIGINT REFERENCES lab_reports(id);

CREATE INDEX idx_medication_events_course ON medication_events(person_id, course_type);
