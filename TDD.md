# Technical Design Document (TDD): Chikkamagaluru Estate Resort Booking & Triage Platform

## 1. Executive Summary & Problem Context

The Chikkamagaluru Estate Resort Booking & Triage Platform is a full-stack hospitality application engineered for a boutique luxury coffee plantation resort in Chikkamagaluru, Karnataka, India. The property offers 76 physical units across two distinct accommodation types: **Estate Rooms** (64 units) and **Private Pool Villas** (12 units). 

The platform fulfills two primary operational needs:
1. **Public Guest Journey**: A high-conversion, mobile-responsive, three-stage booking experience with atomic inventory allocation, dynamic seasonal rate calculation, provisional GST taxation, and deterministic 10-minute hold guarantees.
2. **Staff Enquiry Triage & Operations**: An intelligent triage hub that intakes multi-channel guest inquiries (Web Form, simulated Email, simulated WhatsApp), processes them via Google Gemini with privacy redaction and strict hallucination guardrails, and routes them to dedicated department specialty lanes (Rooms, Weddings, Corporate Offsites, and GM Escalation) with mandatory human review before dispatch.

---



---

## 1.1 Requirements Provenance & Brief Disclosure

> **Explicit Disclosure of Brief Availability**:
> The standalone upstream assignment brief (referred to historically as "the original brief" or "IsoCode Labs brief") is **not present as a discrete file** within this repository or the `docs/` directory.
>
> All verified architectural, domain, and behavioral specifications in this TDD are strictly derived from the committed project documentation:
> - `RULES.md`: Core developer constraints, database immutability, role validation, BigDecimal currency requirements, and testing criteria.
> - `docs/design-pack.md`: Core Build blueprint (v2), 12 relational database tables, room category grouping (64 Rooms + 12 Villas), staff lanes, and enquiry triage rules.
> - `docs/design-pack-patch.md` & `docs/design-pack-v2-patch.md`: Authoritative overrides for GST single-slab calculation (post-promo value of supply), concurrency deadlock retry, active hold capping, and rate limiter sizing.
> - `docs/design-direction-patch.md`: WCAG AA color contrast ratios, 44px tap targets, tokens, responsive viewports (375px mobile, 768px tablet, 1024px+ desktop), and room page requirements.
> - `docs/frontend-rules.md`: Component boundaries, local date handling, idempotent booking submission, and three-stage booking funnel rules.
> - `docs/gst-source.md`: Statutory references (CGST Act s.15, Gazette notifications) for 5% and 18% hospitality tax brackets.
>
> **Requirements Categorization**:
> 1. **Explicitly Required by Project Documentation**:
>    - 76 physical keys (64 Estate Rooms + 12 Private Pool Villas).
>    - Two primary system roles: Guest (public) and Reservations Staff (`ROLE_STAFF` with `is_gm` privilege flag).
>    - Three-stage guest checkout journey: Guest Details (`/checkout`) → Payment (`/payment/:ref`) → Confirmation (`/bookings/:ref`).
>    - 10-minute pessimistic inventory hold guarantee with token-based hold cancellation (`/cancel-hold`).
>    - Token-bucket IP rate limiting (10 RPM bookings, 5 per 10 minutes for public enquiries).
>    - Single GST slab lookup based on post-promo average nightly tariff on check-in date.
>    - Provisional GST slab seeding flagged `UNVERIFIED` with UI disclaimers.
>    - Functional unique index on resort-wide blackout dates (`uk_blackout_dates_scope_date`).
>    - Multi-channel enquiry intake with privacy PII redaction and Gemini AI draft generation.
>    - Rate-table quote injection (`QuoteInjectorService`) preventing hallucinated prices in staff drafts.
>    - Mandatory staff review before dispatch (`DRAFT_PENDING_REVIEW`).
>    - Daily CSV booking export with spreadsheet formula injection protection (CWE-1236).
>    - Mobile-first responsive UI (375px+ without horizontal scroll).
> 2. **Inferred from Implementation & Architecture**:
>    - Mapping 76 physical keys to two bookable room categories (`ROOM` and `VILLA`).
>    - Dynamic port binding for Railway container environments (`SERVER_PORT=${PORT}`).
>    - Automated Vercel Edge deployments via GitHub webhook integration on `main`.
> 3. **NOT VERIFIED (Original Brief Unavailable)**:
>    - Any hypothetical requirements or evaluation criteria belonging strictly to an uncommitted external document that were not transcribed into the committed `docs/` files are marked **NOT VERIFIED**.

## 2. High-Level System Architecture

The application adopts a modular 3-tier architecture with clear separation of concerns:

```
[ Client Layer (Edge CDN / Browser) ]
  ├── React 18 SPA (Vite)
  ├── Edge Routing & Security Headers (Vercel)
  └── Responsive Viewports (390px Mobile, 768px Tablet, Desktop)
         │
         │ HTTPS / REST (JSON)
         v
[ Application Tier (Compute) ]
  ├── Spring Boot 3.3.5 (Java 21 LTS)
  ├── Security Filter Chain (Stateless HS256 JWT Auth)
  ├── RateLimitingFilter (Token Bucket IP Rate Limiting)
  ├── Controllers (Public Booking, Public Enquiries, Admin Triage, Admin Export)
  ├── Domain Services (Booking, Pricing, EnquiryRouting, GeminiAi, QuoteInjector)
  └── Integration Adapters (Gemini REST Client, Simulated Delivery Adapter)
         │
         │ JDBC (MySQL Protocol)
         v
[ Data & Persistence Tier ]
  ├── Persistent MySQL 8.0 (Railway Dedicated Volume)
  ├── Flyway Schema Migrations (V1 through V7)
  └── InnoDB Tables with Foreign Keys, Check Constraints & Functional Indexes
```

### Component Responsibilities

1. **Frontend SPA (`frontend/`)**: Single-page application built with React 18, Vite, React Router 6, and modern CSS modules. Uses accessible semantic HTML, responsive viewport scaling, SVG vector iconography, and client-side validation (e.g. Indian 10-digit mobile phone pattern `^[6-9]\d{9}$`).
2. **Backend API (`backend/`)**: Spring Boot 3 web application running on OpenJDK 21 LTS. Provides REST endpoints organized into `/api/v1/public/**` (unauthenticated) and `/api/v1/admin/**` (protected by `ROLE_STAFF` authority and lane-level access checks).
3. **Database (`MySQL 8.0`)**: Relational storage enforcing referential integrity, transaction ACID isolation, strict ANSI `CHECK` constraints, and functional unique indexes (e.g. resort-wide blackout uniqueness).

---

## 3. Relational Data Model & Schema Design

Database migrations are managed strictly through Flyway scripts (`backend/src/main/resources/db/migration/`):

### Core Entities & Relationships

| Entity / Table | Primary Key | Key Attributes | Constraints & Indexes |
| :--- | :--- | :--- | :--- |
| `users` | `id` (BIGINT) | `email`, `password_hash`, `role`, `specialty_lane`, `is_gm` | Unique on `email`. Role enum (`STAFF`, `ADMIN`). |
| `room_categories` | `id` (BIGINT) | `code`, `name`, `total_inventory`, `max_adults`, `max_children` | Unique on `code` (`ROOM`, `VILLA`). Total inventory = 76 units. |
| `physical_units` | `id` (BIGINT) | `room_category_id`, `unit_number` | Unique on `unit_number` (`R-101`..`R-164`, `V-01`..`V-12`). FK to `room_categories`. |
| `rate_plans` | `id` (BIGINT) | `room_category_id`, `code`, `name`, `meal_plan` | Unique on `(room_category_id, code)` (`EP`, `CP`, `MAP`). FK to `room_categories`. |
| `daily_inventory_rates`| `id` (BIGINT) | `room_category_id`, `stay_date`, `base_rate_inr`, `website_allotment_total`, `booked_count` | Unique on `(room_category_id, stay_date)`. `CHECK (booked_count <= website_allotment_total)`. |
| `blackout_dates` | `id` (BIGINT) | `room_category_id` (NULL for resort-wide), `blackout_date`, `reason` | Functional unique index `uk_blackout_dates_scope_date` on `((COALESCE(room_category_id, 0)), blackout_date)`. |
| `promo_codes` | `id` (BIGINT) | `code`, `discount_type`, `discount_value`, `valid_from`, `valid_to`, `is_active` | Unique on `code` (`DIRECT10` [10%], `COFFEE1500` [₹1500]). |
| `gst_tax_slabs` | `id` (BIGINT) | `min_tariff_inr`, `max_tariff_inr`, `gst_percentage`, `verification_status` | Status `UNVERIFIED` (provisional 5% <= 7,500 INR, 18% > 7,500 INR). |
| `bookings` | `id` (BIGINT) | `booking_reference`, `guest_name`, `guest_email`, `guest_phone`, `check_in_date`, `check_out_date`, `total_amount_inr`, `status`, `expires_at` | Unique on `booking_reference`. Status enum (`PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `EXPIRED`). |
| `payments` | `id` (BIGINT) | `booking_id`, `payment_reference`, `amount_inr`, `status`, `one_time_payment_token` | Unique on `payment_reference` and `one_time_payment_token`. |
| `enquiries` | `id` (BIGINT) | `enquiry_code`, `source_channel`, `sender_name`, `sender_contact`, `category`, `ai_confidence`, `assigned_to_user_id`, `status` | Unique on `enquiry_code`. Enums for channel, category, routing method. |
| `enquiry_drafts` | `id` (BIGINT) | `enquiry_id`, `draft_body`, `quoted_rate_breakdown_json`, `generated_by`, `status`, `sent_by_user_id` | FK to `enquiries` and `users`. Status enum (`DRAFT_PENDING_REVIEW`, `SENT_MANUALLY`, `DISCARDED`). |

---

## 4. Main System Workflows

### 4.1 Three-Stage Guest Booking Funnel

1. **Stage 1: Selection & Guest Details (`/stay` & `/checkout`)**
   - Guest selects stay dates (`checkIn`, `checkOut`), category (`ROOM` or `VILLA`), and rate plan.
   - Live availability is checked against `daily_inventory_rates` and `blackout_dates`.
   - Guest inputs name, email, and 10-digit mobile number.
   - Client-side countdown timer displays a 10-minute hold window.
   - `POST /api/v1/public/bookings` invokes `BookingTransactionService`:
     - Opens database transaction with pessimistic locking (`PESSIMISTIC_WRITE` on `daily_inventory_rates`).
     - Verifies `website_allotment_total - booked_count >= requested_rooms` across all nights.
     - Increments `booked_count` and creates `bookings` row with status `PENDING_PAYMENT` and `expires_at = now() + 10 minutes`.
     - Generates a cryptographically secure single-use `one_time_payment_token`.

2. **Stage 2: Payment Guarantee (`/payment/:bookingRef`)**
   - Renders order summary, rate breakdown, and provisional GST notice.
   - Interacts with internal payment simulator (`POST /api/v1/public/payments/{ref}/mock-webhook`).
   - Verifies the single-use token; transitions booking to `CONFIRMED` upon simulated success.
   - If hold expires before payment (via `BookingExpiryService`), allotment is safely rolled back (`booked_count = booked_count - rooms`) and token is invalidated.

3. **Stage 3: Confirmation Receipt (`/bookings/:bookingRef`)**
   - Displays confirmed booking details, reference number, dates, room category, and guest details.

### 4.2 Multi-Channel Enquiry & AI Triage Pipeline

```
[ Inbound Enquiry ] ──> [ PII Redaction ] ──> [ Gemini AI Extraction ]
                              │                         │
                              │ (Key missing or fail)   │ (Success)
                              v                         v
                   [ RuleFallbackService ]     [ Confidence >= 0.80? ]
                              │                     ├── Yes ──> Route to Specialty Lane (Rooms/Weddings/Offsites)
                              │                     └── No  ──> Route to GM Queue (UNSORTED)
                              v
                   [ Deterministic Draft ] ──> [ QuoteInjectorService ] ──> [ Staff Review & Approval ] ──> [ Dispatch ]
```

1. **Ingestion**: Public web form (`POST /api/v1/public/enquiries`) or staff simulator (`POST /api/v1/admin/enquiries/simulate`).
2. **PII Redaction**: `PiiRedactor` sanitizes email addresses, phone numbers, and potential personal identifiers from prompt context prior to calling external AI APIs.
3. **Structured Gemini Inference**: Invokes Google Gemini (`gemini-2.5-flash` / `gemini-1.5-flash`) requesting structured JSON conforming to `GeminiEnquiryExtraction`:
   - `category`: `ROOM_BOOKING`, `WEDDING`, `CORPORATE_OFFSITE`, `COMPLAINT`, `UNSORTED`.
   - `confidence`: Decimal score between `0.00` and `1.00`.
   - `extracted_rooms`, `extracted_guests`, `extracted_nights`, `check_in_date`, `check_out_date`, `budget_amount`.
   - `suggested_reply`: Polite response containing `{{RATE_TABLE_QUOTE_BLOCK}}`.
4. **Hallucination & Safety Guardrails**:
   - `DraftPriceSanitizer`: Strips invented room rates and replaces with authoritative quote placeholder; sanitizes unsupported hold claims ("We have held a room") and unverified meal/activity inclusions ("includes gourmet breakfast and plantation walk").
   - `QuoteInjectorService`: Directly queries authoritative database rates and GST slabs to compute exact tariffs, injecting verifiable numbers only. If dates/rates are unverified, safely defers to manual coordinator review.
   - `BudgetValidator`: Ignores hallucinated budgets unless explicit currency terms or numbers were present in raw guest input.
5. **Specialty Lane Routing**:
   - `COMPLAINT` or low-confidence (< 0.80): Routes directly to General Manager (`gm.demo@companya-estate.test`) with `gm_alerted = true`.
   - `ROOM_BOOKING`: Routed to Reservations Specialist (`rooms.demo@companya-estate.test`).
   - `WEDDING`: Routed to Wedding Specialist (`weddings.demo@companya-estate.test`).
   - `CORPORATE_OFFSITE`: Routed to Offsites Specialist (`offsites.demo@companya-estate.test`).
6. **Mandatory Human Review**:
   - Draft replies are saved as `DRAFT_PENDING_REVIEW`.
   - No automated email, SMS, or simulated dispatch occurs until authorized staff explicitly reviews, edits, and calls `POST /api/v1/admin/enquiries/{id}/approve-draft`.
   - Atomic draft claiming (`atomicClaimDraft`) prevents race conditions during concurrent staff approvals.

---

## 5. Security & Privacy Architecture

1. **Stateless Authentication**: Staff sessions use HMAC-SHA256 JWTs signed with a mandatory 32+ character `JWT_SECRET`. Tokens expire after 8 hours.
2. **Role & Specialty Lane Authorization**:
   - Endpoints under `/api/v1/admin/**` require `ROLE_STAFF`.
   - Fine-grained access control enforces that staff can only review and approve enquiries belonging to their assigned specialty lane. Complaints and unsorted triage are restricted to GM accounts (`is_gm = true`).
3. **Rate Limiting & DoS Protection**:
   - `RateLimitingFilter` employs an in-memory token-bucket algorithm per client IP.
   - Booking creation: 10 requests / minute.
   - Public enquiry submissions: 5 requests / 10 minutes (returns HTTP 429 with `Retry-After: 600`).
   - Input boundaries: `@Size(max = 5000)` enforces raw message payload limits.
4. **Security Headers**:
   - Spring Security and Vercel edge configuration inject `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, and `HSTS`.
5. **Spreadsheet Formula Injection Protection (CWE-1236)**:
   - Daily CSV export (`/api/v1/admin/bookings/export`) sanitizes cells starting with `=`, `+`, `-`, or `@` by prepending a single quote (`'`), protecting staff spreadsheet software.
6. **Data Privacy**:
   - All seeded demo accounts, guests, and contacts utilize RFC 2606 reserved domains (`@companya-estate.test`, `@example.com`, `+9198...`). Zero production personal data or real phone numbers are stored or committed.

---

## 6. Scope Cuts & Non-Commercial Design Decisions

In accordance with the brief's non-commercial portfolio evaluation guidelines:
1. **Mock Payment Simulator**: No production banking rails (Razorpay, Stripe, UPI switches) are attached. Transactions complete via an internal mock webhook simulator (`POST /api/v1/public/payments/{ref}/mock-webhook`).
2. **Simulated Outbound Dispatch**: Outbound guest communication uses internal adapters (`EMAIL_SIM`, `WHATSAPP_SIM`) generating unique event IDs (e.g. `SIM-DEL-XXXXXXXX`). No commercial SMS gateways (Twilio, Gupshup) or SMTP servers are invoked.
3. **Provisional GST Slabs**: Indian GST slabs (5% / 18%) are seeded with `verification_status = 'UNVERIFIED'`, accompanied by mandatory customer and staff disclaimers that tariffs require validation with a certified chartered accountant prior to commercial operation.
4. **Hobby Tier Hosting**: Deployed under Vercel Hobby and Railway Starter tiers for technical portfolio review.

---

## 7. Responsive Viewport Compliance Matrix

All primary guest and staff screens were audited via headless Chrome across standard responsive breakpoints:

| Viewport | Width | Height | Breakpoint Type | Horizontal Overflow | Result |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Mobile** | 390px | 844px | Small Phone (iPhone 14) | `false` (`scrollWidth = 390px`) | **PASS** |
| **Tablet** | 768px | 1024px | Tablet / Portrait (iPad) | `false` (`scrollWidth <= 768px`) | **PASS** |
| **Desktop** | 1280px | 800px | Standard Laptop Viewport | `false` (`scrollWidth <= 1280px`) | **PASS** |

Tested pages: `/` (Landing), `/stay` (Room Selection), `/enquire` (Public Inquiries), `/staff/login` (Staff Auth), `/staff/inbox` (Staff Triage Hub), and `/staff/inventory` (Inventory, Rates & CSV Export).
