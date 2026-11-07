-- ============================================================================
-- V6__blackout_dates_resort_wide_unique.sql: Enforce uniqueness for resort-wide blackouts
-- In MySQL, NULL values are not considered equal by unique constraints.
-- Thus, uk_blackout_dates_cat_date (room_category_id, blackout_date) allowed duplicate
-- resort-wide blackouts (where room_category_id is NULL) on the same date.
-- A functional unique index using COALESCE(room_category_id, 0) guarantees at most one
-- resort-wide blackout (scope 0) and at most one per room category on any date.
-- ============================================================================

ALTER TABLE blackout_dates
    ADD UNIQUE KEY uk_blackout_dates_scope_date ((COALESCE(room_category_id, 0)), blackout_date);
