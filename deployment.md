# Deployment & Operations Guide: Chikkamagaluru Resort Booking

This document details the configuration, deployment, and operational procedures for the Chikkamagaluru Resort Booking platform across local development environments and cloud hosting targets (Vercel, Railway, and MySQL 8).

> **Requirements Provenance & Explicit Disclosure of Brief Availability**:
> The standalone upstream assignment brief (referred to historically as "the original brief" or "IsoCode Labs brief") is **not present as a discrete file** in this repository or the `docs/` directory.
> All operational and deployment specifications documented herein are grounded directly in the committed project documentation (`RULES.md`, `docs/design-pack.md`, `docs/design-pack-patch.md`, `docs/design-pack-v2-patch.md`, `docs/design-direction-patch.md`, `docs/frontend-rules.md`, and `docs/gst-source.md`). Any requirements not present in this documentation are marked **NOT VERIFIED**.

---

## 1. Prerequisites & Toolchain Versions

Ensure the following runtimes and tools are installed and available on your system path:

| Tool | Minimum Version | Verified Version | Verification Command | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Java Development Kit** | OpenJDK 21 LTS | 21.0.11 | `java -version` | Backend runtime (Spring Boot 3.3.5) |
| **Apache Maven** | 3.9+ | 3.9.6 | `mvn -version` | Or use the wrapper located in `backend/` (`.\backend\mvnw.cmd` / `./backend/mvnw`) |
| **Node.js** | 20+ LTS | 24.14.0 | `node -v` | Frontend build and dev server |
| **npm** | 10+ | 10.9.2 | `npm -v` | Frontend package manager |
| **MySQL Server** | 8.0.16 or newer | 8.0.36 | `mysql --version` | **Strict requirement**: ANSI SQL `CHECK` constraints and functional indexes (`COALESCE(...)`) require MySQL >= 8.0.16 |

### Repository Setup
```bash
git clone https://github.com/PrakashMannam/chikkamagaluru-resort-booking.git
cd chikkamagaluru-resort-booking
```

> **Note on Maven Wrapper Location**:
> The Maven wrapper scripts (`mvnw` and `mvnw.cmd`) reside exclusively in `backend/` (with their `.mvn/` configuration). They do not exist at the repository root. Commands invoking the wrapper must either change directory to `backend/` first (`cd backend; .\mvnw.cmd ...`) or specify the relative path from root (`.\backend\mvnw.cmd ...` / `./backend/mvnw ...`).

---

## 2. Local MySQL Database Setup

Create the local application database (`resort`) and the dedicated test database (`resort_test`) using `utf8mb4` encoding:

```sql
CREATE DATABASE IF NOT EXISTS resort CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS resort_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Command-Line Creation

**PowerShell (Windows):**
```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS resort CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE DATABASE IF NOT EXISTS resort_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

**Bash (Linux / macOS):**
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS resort CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE DATABASE IF NOT EXISTS resort_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

---

## 3. Flyway Database Migrations

Schema creation and baseline data initialization are managed strictly through Flyway. When `spring.flyway.enabled=true`, migrations execute automatically on application startup prior to Hibernate schema validation (`spring.jpa.hibernate.ddl-auto=validate`).

Migration scripts reside in `backend/src/main/resources/db/migration/`:

| Version | Migration Script | Purpose & Contents |
| :--- | :--- | :--- |
| `V1` | `V1__init.sql` | Baseline placeholder migration (`SELECT 1;`) ensuring Flyway starts cleanly. |
| `V2` | `V2__schema.sql` | Core schema creating 12 tables (`users`, `room_categories`, `physical_units`, `rate_plans`, `daily_inventory_rates`, `blackout_dates`, `promo_codes`, `gst_tax_slabs`, `bookings`, `payments`, `enquiries`, `enquiry_drafts`) with foreign keys, uniqueness constraints, and ANSI `CHECK` constraints. |
| `V3` | `V3__reference_data.sql` | Static reference data: 2 room categories (`ROOM`, `VILLA`), 76 physical units (`R-101`..`R-164`, `V-01`..`V-12`), 3 rate plans per category (`EP`, `CP`, `MAP`), initial blackout dates (`2026-12-24`, `2026-12-25`), and active promo codes (`DIRECT10`, `COFFEE1500`). |
| `V4` | `V4__gst_slabs.sql` | Provisional GST tax slabs (0.00–7,500.00 INR: 5.00%, >7,500.00 INR: 18.00%, status `UNVERIFIED`). |
| `V5` | `V5__enquiry_ai_fields.sql` | Schema enhancement adding `ai_summary` (TEXT), `extracted_rooms` (INT), and `ai_error_status` (VARCHAR(100)) to the `enquiries` table for automated triage. |
| `V6` | `V6__blackout_dates_resort_wide_unique.sql` | Enforces uniqueness for resort-wide blackout dates via functional unique index `uk_blackout_dates_scope_date` on `((COALESCE(room_category_id, 0)), blackout_date)`, preventing duplicate resort-wide blackouts on the same calendar day. |

> **Immutability Principle**: Applied Flyway migration scripts are strictly immutable and must never be edited retroactively. Any new schema or data changes must be introduced as sequential forward migrations (`V7`, `V8`, etc.).

---

## 4. Environment Variables Reference & Classification

Never commit real secrets, database passwords, or private API keys to source control. Use the following variable names:

### Backend Variables

| Variable Name | Required / Optional | Default Value | Description & Code Enforcement |
| :--- | :--- | :--- | :--- |
| `DB_URL` | **Mandatory** | None | JDBC connection string (e.g. `jdbc:mysql://localhost:3306/resort?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`). Required by `spring.datasource.url`. |
| `DB_USER` | **Mandatory** | None | MySQL database username. Required by `spring.datasource.username`. |
| `DB_PASSWORD` | **Mandatory** | None | MySQL database password. Required by `spring.datasource.password`. |
| `JWT_SECRET` | **Mandatory** | None | Cryptographic secret for signing staff authentication tokens (HMAC-SHA256). **Enforced by `JwtService.init()`: must be at least 32 characters long** after trimming, or backend fails with `IllegalStateException`. |
| `SERVER_PORT` / `PORT` | Optional | `8080` | HTTP server port. Spring Boot binds to `8080` by default. Can be overridden via `-Dserver.port=${PORT:8080}` or `SERVER_PORT=${PORT}`. |
| `CORS_ALLOWED_ORIGIN` | Optional | `http://localhost:5173` | Allowed origin for Cross-Origin Resource Sharing. Must match the exact frontend URL (e.g. `https://chikkamagaluru-resort-booking.vercel.app` in production). |
| `SPRING_PROFILES_ACTIVE` | Optional | `default` | Set to `demo` to activate `DemoDataSeeder` (pre-seeds the 4 demo staff reviewer accounts and initial inventory rates). |
| `FIXED_TODAY` | Optional | None (system clock) | Fixed ISO-8601 calendar date (e.g. `2026-11-02`) used by `PricingService` and inventory queries for reproducible demonstration testing. |
| `RATE_LIMIT_BOOKING_RPM` | Optional | `10` | Maximum booking creation requests permitted per client IP per minute. |
| `RATE_LIMIT_ENQUIRY_MAX_REQUESTS` | Optional | `5` | Maximum public enquiry submissions permitted per client IP per 10-minute window (design-pack §3). |
| `TRUSTED_PROXY_HOPS` | Optional | `1` | Number of upstream reverse proxy hops to traverse when parsing `X-Forwarded-For` for client IP resolution. |
| `GEMINI_API_KEY` | Optional | None | Google Gemini API key for automated AI triage of guest enquiries. If omitted or empty, `RuleFallbackService` automatically provides deterministic rule-based draft responses. |
| `GEMINI_MODEL` | Optional | `gemini-3.8-flash` | Gemini model identifier used for enquiry assistance. |
| `ENQUIRY_CONFIDENCE_THRESHOLD` | Optional | `0.80` | Minimum confidence score required to apply automated AI categorization without human review. |

### Frontend Variables

| Variable Name | Required / Optional | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `VITE_API_URL` | Optional locally / **Mandatory on Vercel** | `""` (empty string) | Public base URL of backend API. In local development, leaving this empty activates Vite's built-in dev server proxy (`/api` → `http://localhost:8080`). In production (Vercel), must be set to `https://resort-backend-production.up.railway.app`. |

### Local Test Runner Variables

| Variable Name | Required / Optional | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `TEST_DB_URL` | Optional (Required for DB tests) | None | JDBC URL for the dedicated test database (e.g. `jdbc:mysql://localhost:3306/resort_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`). If set, activates 15 integration tests across 3 classes (`DatabaseMigrationIntegrationTest` [5 tests], `AllotmentConcurrencyIntegrationTest` [6 tests], and `BlackoutConcurrencyIntegrationTest` [4 tests]). If unset, these tests are skipped cleanly via JUnit `Assumptions`. |
| `TEST_DB_USER` | Optional | `root` | Test database username. |
| `TEST_DB_PASSWORD` | Optional | `""` (empty) | Test database password. |

---

## 5. JWT Secret Configuration & Cryptographic Generation

The backend uses HMAC-SHA256 signing for staff authentication JSON Web Tokens.

### Source Code Validation Requirement
In `backend/src/main/java/com/companya/resort/security/JwtService.java`:
```java
@PostConstruct
public void init() {
    if (jwtSecret == null || jwtSecret.trim().length() < 32) {
        throw new IllegalStateException("JWT_SECRET must be configured and at least 32 characters long");
    }
    this.key = Keys.hmacShaKeyFor(jwtSecret.trim().getBytes(StandardCharsets.UTF_8));
}
```
If `JWT_SECRET` is missing, blank, or fewer than 32 characters after trimming, Spring Boot throws an `IllegalStateException` on startup and terminates.

### Cryptographically Secure Generation

**PowerShell (Windows):**
```powershell
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($bytes)
    $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
}
finally {
    $rng.Dispose()
}
```

**Bash (Linux / macOS):**
```bash
export JWT_SECRET=$(openssl rand -base64 36)
```

---

## 6. Local Startup Instructions

### Step 1: Start the Spring Boot Backend

Open a terminal in `backend/`:

**PowerShell (Windows):**
```powershell
cd backend

# Configure mandatory database connection and JWT secret
$env:DB_URL="jdbc:mysql://localhost:3306/resort?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USER="root"
$env:DB_PASSWORD="your_mysql_password_here"

# (Generate $env:JWT_SECRET using the cryptographically secure snippet from Section 5)

# Optional configuration
$env:CORS_ALLOWED_ORIGIN="http://localhost:5173"
$env:SPRING_PROFILES_ACTIVE="demo"

# Start via backend Maven wrapper
.\mvnw.cmd spring-boot:run
```

**Bash (Linux / macOS):**
```bash
cd backend

export DB_URL="jdbc:mysql://localhost:3306/resort?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USER="root"
export DB_PASSWORD="your_mysql_password_here"
export JWT_SECRET=$(openssl rand -base64 36)
export CORS_ALLOWED_ORIGIN="http://localhost:5173"
export SPRING_PROFILES_ACTIVE="demo"

./mvnw spring-boot:run
```

**Verified Backend Startup State:**
- Port: `http://localhost:8080` (default)
- Health check: `curl http://localhost:8080/health` → `{"status":"ok"}` (HTTP 200)

### Step 2: Start the React / Vite Frontend

Open a separate terminal in `frontend/`:

**PowerShell / Bash:**
```powershell
cd frontend
npm install
npm run dev
```

**Verified Frontend Startup State:**
- Port: `http://localhost:5173`
- Dev server proxy: Requests to `/api/*` are automatically forwarded to `http://localhost:8080` via `frontend/vite.config.js`.

---

## 7. Running the Test Suite

The backend test suite is divided into fast unit/mock tests and MySQL-backed integration tests. The figures below reflect supplied execution results from the test runner:

### Standard Test Suite (226 Tests — No Database Required)
When `TEST_DB_URL` is omitted, JUnit `Assumptions.assumeTrue` skips the three database integration classes (`DatabaseMigrationIntegrationTest`, `AllotmentConcurrencyIntegrationTest`, and `BlackoutConcurrencyIntegrationTest`). All 226 unit, mock, and WebMvc tests run cleanly with 0 failures and 0 errors:

```powershell
# From repository root
cd backend
.\mvnw.cmd test
```

```bash
# Bash
cd backend
./mvnw test
```

### Full Integration Test Suite (241 Tests — With Local Test Database)
When `TEST_DB_URL` points to `resort_test`, all **241 tests** execute across all 26 test classes with 0 failures, 0 errors, and 0 skipped:

```powershell
# PowerShell
cd backend
$env:TEST_DB_URL="jdbc:mysql://localhost:3306/resort_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:TEST_DB_USER="root"
$env:TEST_DB_PASSWORD="your_mysql_password_here"
.\mvnw.cmd test
```

```bash
# Bash
cd backend
export TEST_DB_URL="jdbc:mysql://localhost:3306/resort_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export TEST_DB_USER="root"
export TEST_DB_PASSWORD="your_mysql_password_here"
./mvnw test
```

### Per-Class Test Count Reconciliation (Surefire XML Reports)

| Test Class | Category | Tests Run | Requires MySQL? | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `AdminBlackoutEndpointsWebMvcTest` | WebMvc Mock | 5 | No | Staff blackout HTTP endpoint validation |
| `AdminBlackoutPromoServiceTest` | Service Unit | 14 | No | Blackout and promo business rules |
| `AdminBookingExportServiceTest` | Service Unit | 9 | No | Daily CSV booking export formatting & status mapping |
| `AdminBookingExportWebMvcTest` | WebMvc Mock | 5 | No | Staff CSV export endpoint authorization & headers |
| `AdminInventoryEndpointsWebMvcTest` | WebMvc Mock | 7 | No | Inventory management endpoints |
| `AdminInventoryServiceTest` | Service Unit | 6 | No | Inventory update and allotment service logic |
| `AdminPromoEndpointsWebMvcTest` | WebMvc Mock | 5 | No | Promo code management endpoints |
| `AllotmentConcurrencyIntegrationTest` | Concurrency Integration | 6 | **Yes (`TEST_DB_URL`)** | Multi-threaded allotment booking isolation |
| `AuthAndSecurityTest` | Security Integration | 14 | No | Authentication and role authorization checks |
| `BlackoutConcurrencyIntegrationTest` | Concurrency Integration | 4 | **Yes (`TEST_DB_URL`)** | Multi-threaded blackout race prevention exercising V6 functional unique index in MySQL 8 |
| `BookingEndpointsWebMvcTest` | WebMvc Mock | 10 | No | Public booking endpoints |
| `BookingPaymentServiceTest` | Service Unit | 22 | No | Payment lifecycle and hold management |
| `BookingServiceTest` | Service Unit | 25 | No | Booking creation, validation, hold timeouts |
| `CsvExporterTest` | Utility Unit | 12 | No | RFC 4180 CSV escaping, CRLF, injection prevention |
| `DatabaseMigrationIntegrationTest` | Migration Integration | 5 | **Yes (`TEST_DB_URL`)** | Flyway V1–V6 schema, tables, constraints, functional indexes |
| `DemoDataSeederIntegrationTest` | Seeder Integration | 1 | No | Seeder execution verification |
| `DemoDataSeederTest` | Unit Test | 2 | No | Demo user credential verification |
| `DraftApprovalConcurrencyIntegrationTest` | Concurrency Integration | 2 | No | Concurrent enquiry draft approval deduplication |
| `EnquiryEndpointsWebMvcTest` | WebMvc Mock | 12 | No | Staff and public enquiry endpoints |
| `EnquirySafetyAndParsingTest` | Unit Test | 7 | No | Guest input sanitization and PII safety |
| `EnquiryServiceTest` | Service Unit | 26 | No | Enquiry business logic and state machine |
| `HealthEndpointTest` | WebMvc Mock | 1 | No | `/health` endpoint verification |
| `PricingServiceTest` | Service Unit | 8 | No | Tariff calculation, meal supplements, tax slabs |
| `PublicRoomControllerTest` | WebMvc Mock | 14 | No | Public room category and rate quote endpoints |
| `RateLimitingFilterEnquiryTest` | WebMvc Mock / Unit | 15 | No | Public enquiry and endpoint rate-limiting rules, token bucket, spoofing mitigation, 10-minute window |
| `SimulatedDeliveryAdapterTest` | Service Unit | 4 | No | Email/WhatsApp simulated delivery adapter |
| **TOTAL** | | **241** (226 without DB) | | **100% Pass Rate (0 failures, 0 errors, 0 skipped)** |

---

## 8. Target Cloud Architecture & Railway Port Mechanics

The application is deployed across high-availability managed cloud infrastructure:

```
+-------------------------------------------------------------------------+
|                                                                         |
|                          CLIENT BROWSER                                 |
|                                                                         |
+--------------------+--------------------------------+-------------------+
                     |                                |
   HTTPS Page Loads  |               Direct API Calls | (Cross-Origin)
   & Static Assets   |                                |
                     v                                v
+------------------------------------+  +---------------------------------+
|          VERCEL EDGE CDN           |  |         RAILWAY BACKEND         |
|  chikkamagaluru-resort-booking     |  |    resort-backend-production     |
|            .vercel.app             |  |          .up.railway.app        |
|                                    |  |                                 |
| - React 18 SPA                     |  | - Spring Boot 3.3.5 (Java 21)   |
| - Vercel Rewrites (`vercel.json`)  |  | - Port: ${PORT:8080} dynamic    |
| - Static Assets: WebP / JPG        |  | - CORS: Allowed Vercel Origin   |
| - Tier: Hobby (Non-Commercial Demo)|  | - Auth: Stateless JWT (HS256)   |
+------------------------------------+  +----------------+----------------+
                                                         |
                                        Private Network  | JDBC via TCP
                                        (MySQL Protocol) |
                                                         v
                                        +---------------------------------+
                                        |         RAILWAY MYSQL 8         |
                                        |                                 |
                                        | - Database: `railway`           |
                                        | - Host: mysql.railway.internal  |
                                        | - Port: 3306                    |
                                        | - Storage: Persistent Volume    |
                                        | - Migrations: Flyway V1-V6      |
                                        +---------------------------------+
```

### Vercel Root Directory & Build Mechanics

1. **Root Directory**: `frontend`. In project configurations where the frontend React SPA is housed in a subdirectory, the Vercel Project setting `Root Directory` must be set to `frontend`. This ensures Vercel installs dependencies (`npm install`) and runs the build script (`vite build`) within `frontend/`.
2. **Framework Preset**: `Vite` (with output directory `dist` and single-page routing rewrites defined in `frontend/vercel.json`).
3. **API URL Injection**: `VITE_API_URL` is configured in Vercel project environment variables pointing to `https://resort-backend-production.up.railway.app`.

### Actual Port Binding Mechanics

1. **Local Default**: `8080`. Neither `application.properties` nor `SecurityConfig.java` defines `server.port`, so Spring Boot defaults to `8080`.
2. **Environment Variable Binding**:
   - Spring Boot's relaxed binding natively maps `SERVER_PORT` to `server.port`.
   - Bare `PORT` (which Railway injects) is **not** automatically recognized by Spring Boot without explicit configuration or JVM argument passing.
3. **Railway Port Injection**:
   - In containerized deployments like Railway, the platform dynamically assigns an external port and sets `$PORT`.
   - To bind Spring Boot to this dynamic port, the JVM execution command must pass `-Dserver.port=${PORT:8080}` (or export `SERVER_PORT=${PORT}`).
4. **Explicit Disclosure of Uncertainty**:
   - This repository contains no committed Dockerfiles, `railway.json`, `nixpacks.toml`, or `Procfile`.
   - Railway builds the application using its automated Java/Nixpacks buildpack runner, which typically generates `java -Dserver.port=$PORT ...` automatically.
   - However, because the private Railway web dashboard settings and custom start command override fields cannot be inspected from this local workspace, the exact runtime command string configured in the live Railway service cannot be verified independently.

---

## 9. Live Deployment Status & URLs

| Component | Provider | Live URL | Observed Status |
| :--- | :--- | :--- | :--- |
| **Frontend** | Vercel (Hobby) | `https://chikkamagaluru-resort-booking.vercel.app` | **LIVE & VERIFIED** (HTTP 200, edge-cached) |
| **Backend** | Railway | `https://resort-backend-production.up.railway.app` | **LIVE & HEALTHY** (HTTP 200 on `/health`) |
| **Database** | Railway MySQL 8 | `mysql.railway.internal:3306/railway` | **LIVE & PERSISTENT** (Attached persistent volume) |

> **Deployment Verification**:
> Production deployments on Railway (backend deployment `d688632a-acc8-4236-9479-7906b0714718`) and Vercel (frontend) are live and verified on release commit `e4cb404`. The Technical Design Document is documented in [TDD.md](TDD.md).

---

## 10. Demo Login Accounts

When running with `SPRING_PROFILES_ACTIVE=demo`, the backend automatically seeds four staff accounts with password:
```text
DEMO-ONLY-NOT-REAL-PASS-2026!
```

| Email | Full Name | Role | Specialty Lane | Permissions & Scope |
| :--- | :--- | :--- | :--- | :--- |
| `rooms.demo@companya-estate.test` | Ananya Hegde (Reservations) | `STAFF` | `ROOM` | Front Office reservations, rate quotes, draft approval, and enquiry simulator |
| `gm.demo@companya-estate.test` | Vikram Rao (GM) | `STAFF` | `GM_ESCALATION` (`is_gm=true`) | Full management access, GM alert banner, guest complaints queue (`COMPLAINT`), and escalation approval |
| `weddings.demo@companya-estate.test` | Karthik Urs (Weddings) | `STAFF` | `WEDDING` | Wedding and banquet lawn enquiries |
| `offsites.demo@companya-estate.test` | Meera Nair (Corporate Offsites) | `STAFF` | `OFFSITE` | Corporate offsite and retreat enquiries |

Staff portal login: `/staff/login`.

---

## 11. Mock Payment & Simulated Enquiry Delivery Limitations

1. **Mock Payment Simulator**:
   - Guest booking checkout transitions bookings from `PENDING_PAYMENT` to `CONFIRMED` via an internal simulated webhook (`POST /api/v1/public/payments/{ref}/mock-webhook`).
   - No external banking networks, credit card gateways (Stripe, Razorpay), or UPI switches are connected.
   - Payment tokens are single-use (`one_time_payment_token`) and invalidated immediately upon transaction completion or hold expiration.
2. **Simulated Enquiry Dispatch**:
   - The staff enquiry simulator (`POST /api/v1/admin/enquiries/simulate`) records mock deliveries under channels `EMAIL_SIM` and `WHATSAPP_SIM`.
   - Outbound dispatch events generate local log entries with unique IDs (e.g. `SIM-DEL-XXXXXXXX`).
   - No actual SMS messages, WhatsApp messages, or emails are transmitted over commercial carrier networks.

---

## 12. Regulatory Compliance & Hosting Disclaimers

1. **Provisional GST Slabs (`UNVERIFIED`)**:
   - GST tax calculations applied by the platform (e.g. 5% on tariff <= 7,500 INR, 18% on tariff > 7,500 INR) are provisional and explicitly flagged `UNVERIFIED` in the database.
   - The checkout interface presents a mandatory disclaimer that tax figures are provisional and must be verified by property management with a certified chartered accountant prior to commercial operation.
2. **Vercel Hobby Plan Terms**:
   - The live frontend deployment is hosted under Vercel's personal **Hobby** tier and is intended strictly for non-commercial portfolio evaluation, technical review, and demonstration.
   - Commercial operation requires an upgraded Vercel plan (Pro or Enterprise) in compliance with the [Vercel Fair Use Guidelines](https://vercel.com/docs/limits/fair-use-guidelines).
3. **Public Enquiry Rate Limiting & Message Boundaries**:
   - `POST /api/v1/public/enquiries` is protected by IP-based token-bucket rate limiting enforcing 5 requests per 10 minutes per IP (configurable via `RATE_LIMIT_ENQUIRY_MAX_REQUESTS`, returning HTTP 429 with `Retry-After: 600`).
   - Raw enquiry message inputs are strictly bounded to a maximum of 5,000 characters via `@Size(max = 5000)`.
4. **Three-Step Booking Journey**:
   - The booking checkout funnel strictly implements the three-step booking requirement:
     - Stage 1: Guest Details & Consent (`/checkout`, `Step 1 of 3`)
     - Stage 2: Payment Guarantee (`/payment/:ref`, `Step 2 of 3 • Payment`)
     - Stage 3: Confirmation Receipt (`/bookings/:ref`, `Step 3 of 3 • Confirmation`)

---

## 13. Static Image Asset Audit

- **Tracked Assets (Required by Code)**: `frontend/public/images/` contains tracked assets (`hero-estate.webp`, `room-canopy.webp`, `villa-pool.webp`, `coffee-plantation-cherries.jpg`, `wedding-lawn-estate.jpg`, `corporate-offsite-estate.jpg`, etc.) referenced directly in UI components.
- **Untracked Local Assets (Auxiliary Images)**: Five auxiliary JPEG images (`coffee-cherries.jpg`, `coffee-villa.jpg`, `corporate-dining.jpg`, `hills-panoramic.jpg`, `wedding-lawn.jpg`) reside locally in `frontend/public/images/`. They are not referenced by application markup, styles, or code and are not required for clean production builds.

---

## 14. Post-Deployment Verification Commands

Run the following checks against any deployed environment:

```bash
# 1. Verify backend health
curl -s -i https://resort-backend-production.up.railway.app/health

# 2. Verify CORS preflight headers
curl -s -i -X OPTIONS "https://resort-backend-production.up.railway.app/health" \
  -H "Origin: https://chikkamagaluru-resort-booking.vercel.app" \
  -H "Access-Control-Request-Method: GET"

# 3. Verify public room inventory query
curl -s "https://resort-backend-production.up.railway.app/api/v1/public/rooms"

# 4. Verify public pricing calculation
curl -s "https://resort-backend-production.up.railway.app/api/v1/public/pricing/quote?roomCategoryId=1&ratePlanId=1&checkIn=2026-11-13&checkOut=2026-11-16"

# 5. Verify frontend root and SPA route resolution
curl -s -I https://chikkamagaluru-resort-booking.vercel.app/
curl -s -I https://chikkamagaluru-resort-booking.vercel.app/stay
curl -s -I https://chikkamagaluru-resort-booking.vercel.app/staff/login
```

---

## 15. Troubleshooting Common Issues

| Symptom | Probable Cause | Resolution |
| :--- | :--- | :--- |
| `IllegalStateException: JWT_SECRET must be configured and at least 32 characters long` | `JWT_SECRET` is unset, blank, or fewer than 32 characters after trimming. | Generate a secure 32+ character key using the cryptographically secure PowerShell snippet from Section 5 and export as `JWT_SECRET`. |
| `Table 'resort.blackout_dates' doesn't exist` | Flyway migrations were disabled or failed to run. | Ensure `spring.flyway.enabled=true` in `application.properties` and verify database user permissions allow `CREATE TABLE`. |
| `Access denied for user 'root'@'localhost'` | Incorrect MySQL credentials. | Verify `DB_USER` and `DB_PASSWORD` match your local MySQL configuration. |
| Browser shows CORS error when making API requests | `CORS_ALLOWED_ORIGIN` does not match the frontend origin. | Ensure `CORS_ALLOWED_ORIGIN` exactly matches the requesting URL without a trailing slash (e.g. `http://localhost:5173` locally or `https://chikkamagaluru-resort-booking.vercel.app` on Railway). |
| Port binding collision: `Web server failed to start. Port 8080 was already in use.` | Another process is holding port 8080. | Stop conflicting process or run Spring Boot with a custom port: `cd backend; .\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"`. |
| Integration tests skipped (`Tests run: 226`) | `TEST_DB_URL` environment variable is not exported. | Export `$env:TEST_DB_URL="jdbc:mysql://localhost:3306/resort_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"` before running `mvn test`. |
| Functional index syntax error on migration `V6` | MySQL version is older than 8.0.16. | Upgrade MySQL Server to version 8.0.16 or newer. Functional indexes and ANSI `CHECK` constraints require MySQL 8.0.16+. |
