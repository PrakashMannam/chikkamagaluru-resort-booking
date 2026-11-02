-- ============================================================================
-- V2__schema.sql: Core schema (12 tables)
-- Based on Section 2 of Core Build Design Pack (v2)
-- ============================================================================

-- 1. users: Staff & GM accounts
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role ENUM('STAFF') NOT NULL,
    specialty_lane ENUM('ROOM', 'WEDDING', 'OFFSITE', 'GM_ESCALATION') NOT NULL,
    is_gm BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. room_categories: Rooms & Villas inventory categories
CREATE TABLE room_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    total_physical_keys INT NOT NULL,
    max_adults INT NOT NULL,
    max_children INT NOT NULL,
    base_description TEXT,
    photo_urls_json JSON
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. physical_units: The 76 physical keys (64 rooms + 12 villas)
CREATE TABLE physical_units (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_category_id BIGINT NOT NULL,
    unit_number VARCHAR(50) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_physical_units_category FOREIGN KEY (room_category_id) REFERENCES room_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. rate_plans: Meal & pricing plans per category (EP, CP, MAP)
CREATE TABLE rate_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_category_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    price_modifier_type ENUM('FIXED_ADDON', 'PERCENTAGE') NOT NULL,
    price_modifier_value DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_rate_plans_category FOREIGN KEY (room_category_id) REFERENCES room_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. daily_inventory_rates: Daily base rate & website allotment cap per category + stay date
-- NOTE: A check that website_allotment_total <= total_physical_keys cannot be a CHECK across tables; leave it for the service layer.
CREATE TABLE daily_inventory_rates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_category_id BIGINT NOT NULL,
    stay_date DATE NOT NULL,
    base_rate_inr DECIMAL(10,2) NOT NULL,
    website_allotment_total INT NOT NULL,
    booked_count INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_daily_inventory_rates_category FOREIGN KEY (room_category_id) REFERENCES room_categories(id),
    CONSTRAINT uk_daily_inventory_rates_cat_date UNIQUE (room_category_id, stay_date),
    CONSTRAINT chk_daily_inventory_rates_booked_count CHECK (booked_count >= 0 AND booked_count <= website_allotment_total)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. blackout_dates: Blackout dates (room_category_id NULL = entire resort)
CREATE TABLE blackout_dates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_category_id BIGINT NULL,
    blackout_date DATE NOT NULL,
    reason VARCHAR(255),
    CONSTRAINT fk_blackout_dates_category FOREIGN KEY (room_category_id) REFERENCES room_categories(id),
    CONSTRAINT uk_blackout_dates_cat_date UNIQUE (room_category_id, blackout_date),
    INDEX idx_blackout_dates_cat_date (room_category_id, blackout_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. promo_codes: Discount promotions
CREATE TABLE promo_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    discount_type ENUM('PERCENTAGE', 'FLAT_INR') NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    min_nights INT NOT NULL DEFAULT 1,
    max_uses INT NOT NULL,
    current_uses INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. gst_tax_slabs: Admin-configured GST rules
CREATE TABLE gst_tax_slabs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    min_tariff_inr DECIMAL(10,2) NOT NULL,
    max_tariff_inr DECIMAL(10,2) NULL,
    gst_percentage DECIMAL(5,2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    verification_status ENUM('UNVERIFIED', 'VERIFIED') NOT NULL DEFAULT 'UNVERIFIED'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. bookings: Guest booking records
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_ref VARCHAR(12) NOT NULL UNIQUE,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    room_category_id BIGINT NOT NULL,
    rate_plan_id BIGINT NOT NULL,
    promo_code_id BIGINT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    nights INT NOT NULL,
    rooms_count INT NOT NULL DEFAULT 1,
    adults INT NOT NULL,
    children INT NOT NULL DEFAULT 0,
    guest_name VARCHAR(255) NOT NULL,
    guest_email VARCHAR(255) NOT NULL,
    guest_phone VARCHAR(50) NOT NULL,
    dpdp_consent_given BOOLEAN NOT NULL DEFAULT FALSE,
    dpdp_consent_timestamp TIMESTAMP NULL,
    subtotal_before_discount_inr DECIMAL(10,2) NOT NULL,
    discount_amount_inr DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    taxable_amount_inr DECIMAL(10,2) NOT NULL,
    gst_percentage_applied DECIMAL(5,2) NOT NULL,
    gst_amount_inr DECIMAL(10,2) NOT NULL,
    total_payable_inr DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING_PAYMENT', 'CONFIRMED', 'FAILED', 'EXPIRED', 'CANCELLED') NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_category FOREIGN KEY (room_category_id) REFERENCES room_categories(id),
    CONSTRAINT fk_bookings_rate_plan FOREIGN KEY (rate_plan_id) REFERENCES rate_plans(id),
    CONSTRAINT fk_bookings_promo_code FOREIGN KEY (promo_code_id) REFERENCES promo_codes(id),
    INDEX idx_bookings_guest_email (guest_email),
    INDEX idx_bookings_guest_phone (guest_phone),
    INDEX idx_bookings_status_expires_at (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. payments: Test-mode payment records with single-use token
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    one_time_payment_token VARCHAR(64) NOT NULL UNIQUE,
    token_used BOOLEAN NOT NULL DEFAULT FALSE,
    token_used_at TIMESTAMP NULL,
    mock_gateway_ref VARCHAR(255) NULL,
    amount_inr DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) NULL,
    status ENUM('INITIATED', 'SUCCESS', 'FAILURE') NOT NULL,
    paid_at TIMESTAMP NULL,
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. enquiries: Guest enquiries across web, simulated email and WhatsApp
CREATE TABLE enquiries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enquiry_code VARCHAR(50) NOT NULL UNIQUE,
    source_channel ENUM('WEB_FORM', 'EMAIL_SIM', 'WHATSAPP_SIM') NOT NULL,
    sender_name VARCHAR(255) NOT NULL,
    sender_contact VARCHAR(255) NOT NULL,
    raw_message TEXT NOT NULL,
    category ENUM('ROOM_BOOKING', 'WEDDING', 'CORPORATE_OFFSITE', 'COMPLAINT', 'UNSORTED') NOT NULL,
    ai_confidence DECIMAL(4,3) NULL,
    routing_method ENUM('AI_HIGH_CONFIDENCE', 'AI_LOW_CONFIDENCE_GM', 'RULE_FALLBACK', 'MANUAL_OVERRIDE', 'SEEDED_STATIC') NOT NULL,
    extracted_check_in DATE NULL,
    extracted_check_out DATE NULL,
    extracted_nights INT NULL,
    extracted_guests INT NULL,
    extracted_budget_inr DECIMAL(12,2) NULL,
    assigned_to_user_id BIGINT NULL,
    gm_alerted BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('OPEN', 'REPLIED', 'CLOSED') NOT NULL DEFAULT 'OPEN',
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    first_replied_at TIMESTAMP NULL,
    CONSTRAINT fk_enquiries_assigned_user FOREIGN KEY (assigned_to_user_id) REFERENCES users(id),
    INDEX idx_enquiries_category_status (category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. enquiry_drafts: AI or staff drafted replies
CREATE TABLE enquiry_drafts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enquiry_id BIGINT NOT NULL,
    draft_body TEXT NOT NULL,
    quoted_rate_breakdown_json JSON NULL,
    generated_by ENUM('AI_ROUTER', 'RULE_TEMPLATE', 'STAFF_EDIT', 'SEEDED_STATIC') NOT NULL,
    status ENUM('DRAFT_PENDING_REVIEW', 'SENT_MANUALLY', 'DISCARDED') NOT NULL DEFAULT 'DRAFT_PENDING_REVIEW',
    sent_by_user_id BIGINT NULL,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enquiry_drafts_enquiry FOREIGN KEY (enquiry_id) REFERENCES enquiries(id),
    CONSTRAINT fk_enquiry_drafts_sent_by_user FOREIGN KEY (sent_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
