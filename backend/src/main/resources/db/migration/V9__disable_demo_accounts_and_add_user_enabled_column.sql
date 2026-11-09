-- ============================================================================
-- V9__disable_demo_accounts_and_add_user_enabled_column.sql
-- Security hardening: add is_active column to users and deactivate
-- publicly exposed shared demo accounts.
-- ============================================================================

ALTER TABLE users
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Disable shared demo accounts whose credentials were leaked or publicly exposed
UPDATE users
SET is_active = FALSE
WHERE email IN (
    'gm.demo@companya-estate.test',
    'rooms.demo@companya-estate.test',
    'weddings.demo@companya-estate.test',
    'offsites.demo@companya-estate.test'
);
