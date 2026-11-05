-- ============================================================================
-- V5__enquiry_ai_fields.sql: Enhance enquiries table for AI processing
-- Adds ai_summary, extracted_rooms and ai_error_status
-- ============================================================================

ALTER TABLE enquiries
    ADD COLUMN ai_summary TEXT NULL AFTER raw_message,
    ADD COLUMN extracted_rooms INT NULL AFTER extracted_guests,
    ADD COLUMN ai_error_status VARCHAR(100) NULL AFTER gm_alerted;
