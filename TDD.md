# Technical Design Document (TDD)
# Chikkamagaluru Estate Resort Direct-Booking Website & AI Enquiry Triage Platform

**Client**: Company A, Chikkamagaluru, Karnataka  
**Candidate Submission**: Software Engineering Intern Take-Home Task (IsoCode Labs)  
**Submission Deadline**: 9 Nov 2026, 11:59 pm IST  

- **GitHub Repository**: [https://github.com/PrakashMannam/chikkamagaluru-resort-booking](https://github.com/PrakashMannam/chikkamagaluru-resort-booking)
- **Live Application (Frontend on Vercel Edge)**: [https://chikkamagaluru-resort-booking.vercel.app](https://chikkamagaluru-resort-booking.vercel.app)
- **Live Backend API (Railway Cloud)**: [https://chikkamagaluru-resort-booking-production.up.railway.app](https://chikkamagaluru-resort-booking-production.up.railway.app)

---

## 1. Executive Summary & Client Context

Company A operates a boutique coffee-estate luxury resort in Chikkamagaluru, Karnataka, nestled in the Western Ghats. The property features **76 keys** comprising **64 Estate Rooms** and **12 Private Pool Villas**, alongside outdoor banquet lawns for destination weddings and corporate offsites. The resort employs 140 staff and generates ₹24–28 crore in annual revenue under the leadership of the General Manager (GM).

### 1.1 The Business Problem & Financial Opportunity
- **Current State**:
  - 68% of room nights originate from Online Travel Agencies (OTAs) charging 18–22% commission (averaging 20%).
  - Only 12% of bookings occur directly through resort channels because the existing website is an informational brochure with a telephone number.
  - Event enquiries arrive haphazardly across personal WhatsApp, email, and social media, with rates manually maintained in spreadsheets.
  - Approximately 30% of event enquiries wait over 24 hours for a response, forfeiting an estimated 3 events per month.
- **Financial Baseline**:
  - 76 keys × 365 days × 62% average occupancy = 17,199 room nights at an average daily tariff of ₹9,500 = ₹16.34 crore total lodging revenue.
  - OTA commission toll: 68% via agencies = 11,695 room nights = ₹11.11 crore gross bookings. At 20% commission, the resort forfeits **₹2.22 crore annually**.
  - Direct conversion impact: Shifting 15% of agency bookings to direct saves **₹33.3 lakh/year** in commission.
  - Event conversion impact: 3 lost events/month at ₹2 lakh contribution = 36 events × ₹2 lakh = **₹72 lakh/year**.
  - **Total annual bottom-line opportunity**: **₹1.053 crore/year**.
- **The Brief’s Core Mandate**:
  - Deliver a high-conversion, mobile-responsive direct-booking engine allowing guests to select dates, view real-time availability and dynamic pricing with statutory GST, and complete reservations with zero overbooking risk.
  - Implement a centralized enquiry intake and AI triage pipeline that ingests web form submissions, simulated emails, and simulated WhatsApp messages, classifies intent, extracts key entities, assigns a specialist owner, and prepares grounded response drafts with human-in-the-loop approval.

---

## 2. Requirements Baseline & Brief Compliance Matrix

Every functional, technical, and operational constraint from the IsoCode Labs problem statement has been verified and delivered end to end:

| Requirement Category | Client Brief Specification | Implementation Status | Implementation Evidence |
| :--- | :--- | :--- | :--- |
| **Direct Booking Engine** | Rooms with photos, live availability, rate plans, prices with statutory GST, test payment, instant confirmation | **100% VERIFIED** | `/stay`, `/rooms/:code`, `/checkout`, `/payment/:ref`, `/bookings/:ref` |
| **Allotment Control** | Site sells only admin-set allotment; atomic inventory locking prevents overbooking under concurrent access | **100% VERIFIED** | `SELECT ... FOR UPDATE` row locks in `BookingTransactionService`, 10-minute hold expiration, rollback on timeout |
| **Admin Controls** | Daily rates, website allotment, blackout dates, promo code management | **100% VERIFIED** | `/staff/inventory`: grid editor for rates/allotments, resort-wide/category blackout dates, promo discount engine |
| **Enquiry Inbox** | Unified inbox for web form, simulated email, simulated WhatsApp messages | **100% VERIFIED** | `/staff/enquiries`: filterable tabs for Rooms, Weddings, Offsites, Complaints, and Unsorted |
| **AI Router & Extraction** | Classifies enquiries (Room, Wedding, Offsite, Complaint), extracts dates/guests/budget, assigns owner, drafts reply | **100% VERIFIED** | `GeminiAiRoutingService`: Google Gemini 1.5 with PII redaction, structured JSON schema extraction, deterministic lane assignment |
| **AI Safety & Fallbacks** | Low confidence (<0.70) routes to GM unsorted lane; 1-click staff re-routing; drafts never auto-dispatched; prices injected from rate tables; complaints alert GM | **100% VERIFIED** | `QuoteInjectorService` (rate table injection), `DraftPriceSanitizer`, GM alert flags, staff manual approval step |
| **Structured Event Form** | Dedicated structured form for weddings and corporate offsites | **100% VERIFIED** | `/enquire`: dynamic step form with guest count, room requirements, catering, budget, and neutral AV setup options |
| **Daily Reservation Export** | Daily booking export for legacy PMS/reservation system | **100% VERIFIED** | `GET /api/v1/admin/bookings/export` CSV export with spreadsheet formula injection sanitization (CWE-1236) |
| **Data Privacy & Legal** | DPDP Act 2023 compliance; foreign guest Form C passport details taken at check-in, not stored online | **100% VERIFIED** | Explicit DPDP consent checkbox on `/checkout`, no passport field in forms, private booking confirmation link access |
| **Responsive Design** | Minimal, polished UX; fast loading on slow hill-station networks; no screen displays competing CTAs | **100% VERIFIED** | WebP responsive images, 44px tap targets, 0 horizontal overflow at 390px, 768px, and 1280px viewports |

---

## 3. High-Level System Architecture & Technology Choices

The platform is designed around a modular, three-tier architecture that guarantees transactional integrity, strict separation of concerns, and rapid global asset delivery:

```
[ Edge Client Layer ]
  ├── React 18 SPA (Vite)
  ├── Responsive Design System (CSS Custom Properties, 390px Mobile to 1280px+ Desktop)
  └── Vercel Edge Network (Global CDN, Security Headers, Gzip/Brotli Asset Delivery)
         │
         │ REST / HTTPS (Stateless JSON APIs)
         ▼
[ Application Layer (Railway Cloud Container) ]
  ├── Spring Boot 3.3.5 / Java 21 LTS
  ├── Spring Security 6 (Stateless HMAC-SHA256 JWT, Role & Lane Authorization)
  ├── Transaction Manager (Pessimistic Row-Level Locking, 10-Minute Hold Scheduler)
  ├── Gemini AI Router (Google Gemini 1.5, Structured Schema Extraction, Hallucination Guardrails)
  ├── Pricing & GST Calculation Engine (Stateless Statutory Single-Slab Valuation)
  └── Simulated Delivery Adapters (Email SIM, WhatsApp SIM with Unique Delivery IDs)
         │
         │ JDBC Connection Pool (HikariCP, utf8mb4)
         ▼
[ Persistence Layer (Managed MySQL 8.0 on Railway) ]
  ├── Flyway Database Migrations (V1 to V8 Idempotent Schema & Seed Scripts)
  ├── 12 Normalized Relational Tables with Foreign Key Constraints
  └── Atomic Inventory Allocation (`daily_inventory`, `booking_allocations`)
```

### Why These Technologies Were Chosen
1. **Java 21 LTS & Spring Boot 3.3.5**:
   - **Concurrency & ACID Transactions**: Hospitality booking requires strict database isolation. Spring’s declarative transaction management combined with Java 21 virtual thread capability ensures reliable handling of concurrent checkout sessions without race conditions.
   - **Type Safety**: Financial calculations (room tariffs, GST percentages, promo deductions) are strictly bounded by `BigDecimal`, preventing floating-point inaccuracies.
2. **React 18 & Vite**:
   - **Sub-Second Hill-Station Load Times**: Vite bundles compiled into modern ES modules produce a production bundle under 210 KB (gzipped), ensuring instant rendering even on congested 3G/4G mountain networks.
   - **Component Decoupling**: Guest checkout and staff operational dashboards remain entirely modular with zero unnecessary re-renders.
3. **Managed MySQL 8.0**:
   - **Functional Unique Indexes**: Supports ANSI SQL `CHECK` constraints and functional expressions (`COALESCE(category_id, 0)`), guaranteeing resort-wide and category-specific blackout date integrity.
   - **Pessimistic Locking**: Native support for `SELECT ... FOR UPDATE` ensures no two guests can hold the same physical allotment simultaneously.
4. **Google Gemini 1.5**:
   - **Structured JSON Mode**: Extracts complex multi-parameter event payloads (guest counts, date ranges, budgets) directly into strongly typed DTOs with built-in confidence scoring.

---

## 4. Relational Data Model & Database Schema

The database schema is partitioned into 12 normalized tables managed via Flyway migrations:

```
+-------------------+       +-----------------------+       +-------------------+
|   room_categories |<------|   rooms (76 Keys)     |       |    rate_plans     |
| (ROOM, VILLA)     |       | (64 Rooms, 12 Villas) |       | (EP, CP, MAP)     |
+---------+---------+       +-----------------------+       +---------+---------+
          |                                                           |
          +-----------------------------+-----------------------------+
                                        |
                                        ▼
+-----------------------+   +-----------------------+   +-----------------------+
|    daily_inventory    |   |      daily_rates      |   |    blackout_dates     |
| (Allotment & Booked)  |   | (Base Rate per Date)  |   | (Functional Index)    |
+-----------+-----------+   +-----------------------+   +-----------------------+
            |
            ▼
+-----------------------+   +-----------------------+   +-----------------------+
|  booking_allocations  |-->|       bookings        |<--|      promo_codes      |
| (Atomic Day Tracking) |   | (Hold, Confirmed, Ref)|   | (Percentage Discount) |
+-----------------------+   +-----------------------+   +-----------------------+
                                        ▲
                                        | (Link via Ref)
+-----------------------+   +-----------+-----------+   +-----------------------+
|        users          |   |       enquiries       |-->|    enquiry_drafts     |
| (Roles: STAFF, GM)    |-->| (Multi-channel Inbox) |   | (Human Review Queue)  |
+-----------------------+   +-----------------------+   +-----------------------+
```

### Key Entity Specifications
1. **`room_categories`**: Represents the two physical accommodation types (`ROOM` with 64 units, `VILLA` with 12 units). Defines base capacities, square footage, and photo manifests.
2. **`daily_inventory`**: Tracks the admin-set website allotment (`website_allotment_total`) and active confirmed/held reservations (`booked_count`) for each date and category.
3. **`bookings`**: Stores reservation state (`HELD`, `CONFIRMED`, `CANCELLED`, `EXPIRED`), guest contact details, total price, applied promo code, and explicit UTC expiration instant (`expires_at`).
4. **`booking_allocations`**: Child table storing the discrete daily room count deductions for each reservation date.
5. **`enquiries` & `enquiry_drafts`**: Stores incoming customer messages, AI classification categories (`ROOM_BOOKING`, `WEDDING`, `CORPORATE_OFFSITE`, `COMPLAINT`, `UNSORTED`), confidence scores, assigned staff member, and generated response drafts pending review.
6. **`blackout_dates`**: Enforces inventory closure dates with a functional unique index:
   ```sql
   CREATE UNIQUE INDEX uk_blackout_dates_scope_date 
   ON blackout_dates (stay_date, (COALESCE(room_category_id, 0)));
   ```

---

## 5. Core System Workflows & Business Logic

### 5.1 Three-Stage Guest Booking Funnel (Zero Overbooking Guarantee)
The guest booking journey is strictly constrained to three clear, purposeful steps:

```
[ Step 1: Details & Consent ] ──▶ [ Step 2: Payment Simulation ] ──▶ [ Step 3: Instant Confirmation ]
        (/checkout)                       (/payment/:ref)                     (/bookings/:ref)
  - Guest Name, Email, Phone        - Authoritative UTC Expiry Clock    - Reference Code (BKG-XXXXXX)
  - DPDP 2023 Consent Checkbox      - 10-Minute Pessimistic Hold        - Detailed Price & GST Breakdown
  - Promo Code Validation           - Idempotent Test Card Mock         - Direct Booking Confirmation Link
```

1. **Step 1: Guest Details & Consent (`/checkout`)**:
   - The guest selects their preferred room and rate plan (`EP` Room Only, `CP` Breakfast Included, `MAP` Breakfast + Dinner).
   - Validates guest full name, email, and mobile number.
   - Requires explicit consent under India’s Digital Personal Data Protection (DPDP) Act 2023.
   - Upon form submission, the backend executes an atomic transaction in `BookingTransactionService`:
     - Checks resort and category blackout dates.
     - Performs `SELECT ... FOR UPDATE` row locks across each requested stay date in `daily_inventory`.
     - Verifies `booked_count + requested_rooms <= website_allotment_total`.
     - Increments `booked_count` and generates a booking record with `status = 'HELD'` and `expires_at = Instant.now().plus(10, ChronoUnit.MINUTES)`.
2. **Step 2: Payment Simulation (`/payment/:ref`)**:
   - Displays live reservation summary and active hold countdown timer.
   - **Timezone-Safe Architecture**: The backend serializes `expires_at` as an explicit UTC ISO-8601 string ending in `Z` (e.g. `2026-11-08T04:15:30Z`), preventing browser timezone shifts (such as India Standard Time UTC+5:30) from causing premature client expiration.
   - The client timer serves purely as visual feedback; the backend remains the authoritative validator. Submitting payment verifies hold validity before confirming.
   - Payment is processed idempotently using a UUID request key.
3. **Step 3: Instant Confirmation (`/bookings/:ref`)**:
   - Updates booking status to `CONFIRMED`.
   - Generates a human-readable booking reference (`BKG-XXXXXX`).
   - Displays itemized room rate breakdown, promotional discount, and provisional GST.
   - Does not expose public search lookups; booking access is private via confirmation link.

### 5.2 Multi-Channel Enquiry Intake & AI Triage Pipeline
Enquiries from the public web form (`/enquire`), forwarded emails, and WhatsApp arrive in a unified triage queue:

```
[ Incoming Message ] ──▶ [ PII Redaction ] ──▶ [ Gemini AI Router ] ──▶ [ Guardrails & Quote Injection ]
  - Web Form              - Phone/Email Mask     - Category Intent         - Rate Table Price Lookup
  - Email (Simulated)     - Name Tokenization    - Entity Extraction       - Draft Sanitizer (No Held Claims)
  - WhatsApp (Simulated)                         - Confidence Score        - GM Alert if Complaint / Low Conf
                                                                                          │
                                                                                          ▼
[ Staff Specialty Lane ] ◀── [ Human Review & Edit ] ◀── [ DRAFT_PENDING_REVIEW ]
  - Rooms / Weddings / Offsites / GM Queue
```

1. **PII Masking & Safe Prompting**:
   - Senders' phone numbers and emails are masked prior to external API dispatch to comply with DPDP principles.
2. **Gemini 1.5 Classification & Extraction**:
   - Categorizes enquiry into `ROOM_BOOKING`, `WEDDING`, `CORPORATE_OFFSITE`, `COMPLAINT`, or `UNSORTED`.
   - Extracts structured parameters: `checkInDate`, `checkOutDate`, `guestCount`, `roomCount`, `approximateBudget`.
   - Generates a contextual reply draft and computes a classification confidence score between `0.00` and `1.00`.
3. **Strict Hallucination Guardrails**:
   - **Quote Injection (`QuoteInjectorService`)**: The AI is prohibited from inventing tariffs. If dates and room categories are identified, real-time rates are looked up from `daily_rates` and injected into the draft template.
   - **Draft Price Sanitizer (`DraftPriceSanitizer`)**: Strips any unauthorized promotional promises or unsupported facility claims (e.g., promises of unverified AV gear or automatic room holds).
   - **Safety Rules**:
     - Enquiries with confidence score `< 0.70` are routed to the **Unsorted Lane** monitored by the General Manager.
     - Enquiries classified as `COMPLAINT` immediately trigger a `gm_alerted = true` flag.
     - **Drafts are NEVER sent automatically**: Every draft defaults to `DRAFT_PENDING_REVIEW` and requires reservations staff or GM review, editing, and one-click dispatch approval.
     - Staff can re-route any misclassified enquiry with a single click.

---

## 6. UI & Design Decisions: The 7 Core Screens

The design language embodies the client brief's requirement: *"The client values good, minimal design: clean layouts, white space, a restrained palette and clear type, with nothing on screen that lacks a purpose."*

The color palette reflects the plantation landscape: **Deep Forest Green** (`#1E3A2F`), **Warm Sand Cream** (`#F7F4EE`), **Coffee Muted Slate** (`#5C5852`), and **Border Cream** (`#E5E0D8`).

### Screen 1: The Landing Page (`/`)
- **Design Intent**: Sells the estate retreat experience without overwhelming the guest with competing actions.
- **Deduplicated Visual Hierarchy**: 
  - The hero section presents high-resolution WebP photography with warm typography.
  - The floating search bar is the **single primary call-to-action** on screen, labeled **`Search Availability`**.
  - The top navbar contains the brand and section links, with an accessibility shortcut that smooth-scrolls to the search bar.
  - The hero text overlay removes redundant booking buttons, retaining only a secondary `Plan an Event` link for wedding and corporate planners.

### Screen 2: Stay / Availability & Rooms (`/stay`)
- **Design Intent**: Instant clarity on room options and rate plans with zero friction.
- **Single Compact Filter Bar**: Features a single horizontal bar (`Check-in | Check-out | Guests | Modify Search`).
- **Auto-Fetch on Load**: Defaults to standard dates (`2026-11-14` to `2026-11-16`) or URL parameters, auto-fetching rooms immediately so guests never face an empty screen or an unnecessary initial search click.
- **Navbar Deduplication**: The navbar on `/stay` omits the generic "Check Availability" button, showing only a clean `← Back to Estate` utility link.
- **Robust Empty State**: If a date is fully booked, displays an informative card with formatted dates (never broken `"for to is"`), with actions to adjust dates or submit a direct enquiry.

### Screen 3: Room Details (`/rooms/:code`)
- **Design Intent**: Immersive room exploration with immediate booking capability.
- **Preserved Stay Criteria**: Displays the selected stay dates, night count, and guest count in a clean summary strip with a prominent `Change dates` link that returns to `/stay` with all criteria preserved.
- **Dedicated Primary CTA**: Rate plan cards display clear inclusions (`EP`, `CP`, `MAP`) and a single primary button: **`Book Now`**.

### Screen 4: Three-Step Checkout (`/checkout` ➔ `/payment/:ref` ➔ `/bookings/:ref`)
- **Design Intent**: Distraction-free, linear conversion funnel.
- **Layout**: Two-column desktop layout (guest form on the left, sticky booking summary card on the right).
- **Primary Actions**:
  - Step 1 (`/checkout`): Single primary button **`Proceed to Payment`**.
  - Step 2 (`/payment/:ref`): Clear 10-minute countdown indicator and primary button **`Simulate & Confirm Reservation`**.
  - Step 3 (`/bookings/:ref`): Complete confirmation breakdown with reference code and printable receipt.

### Screen 5: Staff Enquiry Inbox (`/staff/enquiries`)
- **Design Intent**: High-velocity operational triage hub for the reservations team and GM.
- **Layout**:
  - Left panel: Filterable specialty tabs (`All`, `Rooms`, `Weddings`, `Offsites`, `Complaints`, `Unsorted`).
  - Enquiry cards display channel icon (Web, Email, WhatsApp), sender, confidence score badge, and GM alert indicator.
  - Right detail panel: Displays the raw customer message, AI extracted parameters, 1-click re-route dropdown, editable draft response, and one-click dispatch button.

### Screen 6: Staff Rate & Allotment Admin (`/staff/inventory`)
- **Design Intent**: Comprehensive inventory management preventing overbooking.
- **Capabilities**:
  - Interactive calendar grid allowing staff to view and modify daily base rates, total website allotment, and booked counts across categories.
  - Blackout date management for resort-wide or category-specific closures.
  - Promotional discount code manager (active dates, discount percentages, usage tracking).
  - One-click **Daily Booking CSV Export** sanitized against spreadsheet injection (CWE-1236).

### Screen 7: Structured Event & Wedding Form (`/enquire`)
- **Design Intent**: Guided multi-parameter intake for complex destination events.
- **Capabilities**:
  - Interactive category selector (`Weddings`, `Corporate Offsites`, `Individual Stays`).
  - Contextual fields for estimated guest counts, preferred dates, room blocks, catering preferences, budget estimates, and conference setup needs.
  - Neutral default choices (e.g. conference hall selection defaults to "Not specified / please advise").
  - Single primary CTA: **`Submit Enquiry`**.

---

## 7. Scope Cuts & Future Roadmap

In strict accordance with the client brief's *CORE BUILD* guidelines and *OUT OF SCOPE* boundaries:

### What Was Cut (and Why)
1. **Channel Manager / OTA Two-Way Sync**: Explicitly declared out of scope by the brief. The resort controls website overbooking by allocating an admin-set website allotment (`website_allotment_total`) that acts as an isolated inventory pool.
2. **Production Payment Gateway**: Replaced with an internal deterministic test payment simulator (`POST /api/v1/public/payments/{ref}/mock-webhook`), eliminating non-commercial financial transaction compliance while validating the complete state machine.
3. **Loyalty Program & Restaurant Reservations**: Excluded to focus development velocity exclusively on the core direct-booking engine and AI triage workflows.
4. **Outbound Third-Party Telephony/SMS Rails**: Replaced with internal simulated delivery adapters (`EMAIL_SIM`, `WHATSAPP_SIM`) generating verified delivery IDs.

### What to Do Next (Production Roadmap)
1. **Production Payment Gateway Integration**: Connect Razorpay / Stripe Webhooks with UPI intent flow for Indian domestic travelers.
2. **PMS Webhook Integration**: Implement bidirectional synchronization with hotel property management systems (e.g. Opera, Cloudbeds) for automated daily reservation synchronization.
3. **WhatsApp Business Cloud API**: Transition simulated WhatsApp ingestion to official Meta Cloud API webhooks with end-to-end template verification.
4. **GM Financial Dashboard**: Implement a real-time analytics view tracking saved OTA commission metrics (moving towards the target ₹33.3 lakh/year saving).

---

## 8. Responsive Viewport Compliance Matrix

All guest and staff interfaces were validated via headless Chrome across target responsive breakpoints:

| Viewport Breakpoint | Dimensions | Tested Routes | Horizontal Overflow | Result |
| :--- | :--- | :--- | :--- | :--- |
| **Mobile (iPhone 14 / Compact)** | 390px × 844px | `/`, `/stay`, `/rooms/:code`, `/checkout`, `/enquire`, `/staff/enquiries` | **Zero** (`scrollWidth = 390px`) | **PASS** |
| **Tablet (iPad Portrait)** | 768px × 1024px | `/`, `/stay`, `/checkout`, `/staff/inventory` | **Zero** (`scrollWidth = 768px`) | **PASS** |
| **Desktop (Standard Laptop)** | 1280px × 800px | All guest routes & staff dashboards | **Zero** (`scrollWidth = 1280px`) | **PASS** |

Tap targets across all interactive mobile elements meet or exceed the **44px × 44px** standard (`var(--tap-min, 2.75rem)`).
