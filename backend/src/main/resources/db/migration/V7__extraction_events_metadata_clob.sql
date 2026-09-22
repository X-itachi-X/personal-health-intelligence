-- Telemetry metadata stores JSON text previews (up to 4k chars, JSON-escaped).
-- VARCHAR(2048) overflowed on real lab PDFs and aborted extraction after text succeeded.
ALTER TABLE extraction_events ALTER COLUMN metadata CLOB;
