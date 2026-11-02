-- ============================================================================
-- V3__reference_data.sql: Static reference data
-- Based on Section 8 of Core Build Design Pack (v2)
-- ============================================================================

-- 1. Room Categories (ROOM with 64 keys, VILLA with 12 keys)
INSERT INTO room_categories (id, code, name, total_physical_keys, max_adults, max_children, base_description, photo_urls_json) VALUES
(1, 'ROOM', 'Coffee Estate Canopy Room', 64, 2, 2, 'Nestled in the lush coffee plantation canopy with private balcony and serene estate views.', '["https://placehold.co/800x600?text=Estate+Room+1", "https://placehold.co/800x600?text=Estate+Room+2"]'),
(2, 'VILLA', 'Private Plantation Pool Villa', 12, 3, 2, 'Spacious villa featuring a private plunge pool, estate panorama, and luxurious amenities.', '["https://placehold.co/800x600?text=Pool+Villa+1", "https://placehold.co/800x600?text=Pool+Villa+2"]');

-- 2. Physical Units (76 units: R-101..R-164 and V-01..V-12)
INSERT INTO physical_units (room_category_id, unit_number, is_active) VALUES
(1, 'R-101', TRUE), (1, 'R-102', TRUE), (1, 'R-103', TRUE), (1, 'R-104', TRUE), (1, 'R-105', TRUE),
(1, 'R-106', TRUE), (1, 'R-107', TRUE), (1, 'R-108', TRUE), (1, 'R-109', TRUE), (1, 'R-110', TRUE),
(1, 'R-111', TRUE), (1, 'R-112', TRUE), (1, 'R-113', TRUE), (1, 'R-114', TRUE), (1, 'R-115', TRUE),
(1, 'R-116', TRUE), (1, 'R-117', TRUE), (1, 'R-118', TRUE), (1, 'R-119', TRUE), (1, 'R-120', TRUE),
(1, 'R-121', TRUE), (1, 'R-122', TRUE), (1, 'R-123', TRUE), (1, 'R-124', TRUE), (1, 'R-125', TRUE),
(1, 'R-126', TRUE), (1, 'R-127', TRUE), (1, 'R-128', TRUE), (1, 'R-129', TRUE), (1, 'R-130', TRUE),
(1, 'R-131', TRUE), (1, 'R-132', TRUE), (1, 'R-133', TRUE), (1, 'R-134', TRUE), (1, 'R-135', TRUE),
(1, 'R-136', TRUE), (1, 'R-137', TRUE), (1, 'R-138', TRUE), (1, 'R-139', TRUE), (1, 'R-140', TRUE),
(1, 'R-141', TRUE), (1, 'R-142', TRUE), (1, 'R-143', TRUE), (1, 'R-144', TRUE), (1, 'R-145', TRUE),
(1, 'R-146', TRUE), (1, 'R-147', TRUE), (1, 'R-148', TRUE), (1, 'R-149', TRUE), (1, 'R-150', TRUE),
(1, 'R-151', TRUE), (1, 'R-152', TRUE), (1, 'R-153', TRUE), (1, 'R-154', TRUE), (1, 'R-155', TRUE),
(1, 'R-156', TRUE), (1, 'R-157', TRUE), (1, 'R-158', TRUE), (1, 'R-159', TRUE), (1, 'R-160', TRUE),
(1, 'R-161', TRUE), (1, 'R-162', TRUE), (1, 'R-163', TRUE), (1, 'R-164', TRUE),
(2, 'V-01', TRUE), (2, 'V-02', TRUE), (2, 'V-03', TRUE), (2, 'V-04', TRUE), (2, 'V-05', TRUE),
(2, 'V-06', TRUE), (2, 'V-07', TRUE), (2, 'V-08', TRUE), (2, 'V-09', TRUE), (2, 'V-10', TRUE),
(2, 'V-11', TRUE), (2, 'V-12', TRUE);

-- 3. Rate Plans (3 per category: EP +0, CP +1200, MAP +3000 as FIXED_ADDON)
INSERT INTO rate_plans (room_category_id, code, name, price_modifier_type, price_modifier_value, is_active) VALUES
(1, 'EP', 'European Plan (Room Only)', 'FIXED_ADDON', 0.00, TRUE),
(1, 'CP', 'Continental Plan (Breakfast Included)', 'FIXED_ADDON', 1200.00, TRUE),
(1, 'MAP', 'Modified American Plan (Breakfast & Dinner)', 'FIXED_ADDON', 3000.00, TRUE),
(2, 'EP', 'European Plan (Room Only)', 'FIXED_ADDON', 0.00, TRUE),
(2, 'CP', 'Continental Plan (Breakfast Included)', 'FIXED_ADDON', 1200.00, TRUE),
(2, 'MAP', 'Modified American Plan (Breakfast & Dinner)', 'FIXED_ADDON', 3000.00, TRUE);

-- 4. Blackout Dates (2026-12-24 and 2026-12-25, room_category_id NULL for full resort buyout)
INSERT INTO blackout_dates (room_category_id, blackout_date, reason) VALUES
(NULL, '2026-12-24', 'Full Resort Wedding Buyout'),
(NULL, '2026-12-25', 'Full Resort Wedding Buyout');

-- 5. Promo Codes (DIRECT10, COFFEE1500)
INSERT INTO promo_codes (code, discount_type, discount_value, valid_from, valid_to, min_nights, max_uses, current_uses, is_active) VALUES
('DIRECT10', 'PERCENTAGE', 10.00, '2026-11-01', '2027-04-30', 2, 1000, 0, TRUE),
('COFFEE1500', 'FLAT_INR', 1500.00, '2026-11-01', '2027-04-30', 3, 500, 0, TRUE);

-- 6. Placeholder GST Slab (0.00%, UNVERIFIED, effective_from 2026-11-01)
INSERT INTO gst_tax_slabs (min_tariff_inr, max_tariff_inr, gst_percentage, effective_from, effective_to, verification_status) VALUES
(0.00, NULL, 0.00, '2026-11-01', NULL, 'UNVERIFIED');
