-- ============================================================================
-- V4__gst_slabs.sql: Seed realistic GST slabs (UNVERIFIED)
-- Replaces placeholder GST slab from V3
-- ============================================================================

DELETE FROM gst_tax_slabs;

INSERT INTO gst_tax_slabs (min_tariff_inr, max_tariff_inr, gst_percentage, effective_from, effective_to, verification_status)
VALUES
(0.00, 7500.00, 5.00, '2025-09-22', NULL, 'UNVERIFIED'),
(7500.00, NULL, 18.00, '2025-09-22', NULL, 'UNVERIFIED');
