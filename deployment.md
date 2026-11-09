# Deployment & Operations Guide: Chikkamagaluru Resort Booking

This document details the configuration, deployment, and operational procedures for the Chikkamagaluru Resort Booking platform across local development environments and cloud hosting targets (Vercel, Railway, and MySQL 8).

> **Requirements Provenance & IsoCode Labs Brief Alignment**:
> The complete IsoCode Labs Take-Home Assignment Brief for *Company A, Chikkamagaluru* is documented and committed in [`docs/isocode-assignment-brief.md`](docs/isocode-assignment-brief.md).
> This guide documents setup and deployment procedures; it is not a compliance certification. See `TDD.md` for the source-aware requirements matrix, assumptions, audit limits, and release status. Production deployment settings and exact live source revision are not fully verifiable from this repository.

---

## 1. Prerequisites & Toolchain Versions

Ensure the following runtimes and tools are installed and available on your system path:

| Tool | Minimum Version | Observed in Audit Environment (8 Nov 2026) | Verification Command | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Java Development Kit** | OpenJDK 21 LTS | 21.0.12.1 | `java -version` | Backend runtime (Spring Boot 3.3.5) |
| **Apache Maven** | 3.9+ | Maven Wrapper 3.9.9 | `./backend/mvnw -version` | Use the wrapper located in `backend/` (`.\backend\mvnw.cmd` / `./backend/mvnw`). |
| **Node.js** | `^20.19.0` or `>=22.12.0` | 22.23.4 | `node -v` | Vite 7 / React plugin build runtime; this is the audit environment, not a Vercel runtime observation. |
| **npm** | 10+ | 10.9.9 | `npm -v` | Frontend package manager |
| **MySQL Server** | 8.0.16 or newer | Not connected; local client reports 8.0.46 | `mysql --version` | MySQL integration tests were not run in this task; the client version does not prove the production server version. |

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
| `V7` | `V7__correct_seeded_enquiry_drafts.sql` | Idempotent data migration correcting legacy seeded demo enquiry drafts for `ENQ-DEMO-ROOM1` and `ENQ-DEMO-WED1` to grounded, non-committal wording. The latest supplied deployment report records it as applied. |
| `V8` | `V8__correct_corporate_demo_draft.sql` | Idempotent data migration correcting the legacy seeded corporate demo draft. The latest supplied deployment report records it as applied, with deployed schema version v8; the current production source revision remains unverified. |

> **Immutability Principle**: Applied Flyway migration scripts are strictly immutable and must never be edited retroactively. Any new schema or data changes must be introduced as sequential forward migrations after the highest applied version (currently v8 in the latest supplied deployment report).

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

## 7. Automated Test Evidence (8–9 November 2026)

The following results are from repository `main` commit `296100f`, which incorporates both PR #12 (hardened concurrency verification) and PR #13 (AI tax-grounding on unverified GST). Both PRs are merged and deployed.

**Backend:**
- Baseline test suite (`cd backend && ./mvnw -q test` without database): **282 tests passed**, 0 failures, 0 errors, 0 skipped across 26 test classes.
- MySQL-backed integration test suite executed against disposable MySQL 8.0.45 (`disposable_concurrency_repeat_test_db`): **298 tests passed**, 0 failures, 0 errors, 0 skipped across 29 test classes with `TEST_DB_URL` configured. This includes 16 database integration tests: `AllotmentConcurrencyIntegrationTest` (9 tests, including 100 repeated runs of competing holds for the last room with zero overselling, multi-night overlapping ranges, idempotency races, payment vs expiry races, and rollback assertions), `BlackoutConcurrencyIntegrationTest` (4 tests), and `DatabaseMigrationIntegrationTest` (5 tests, Flyway V1–V8).
- Hardened database guard: `DisposableDatabaseGuardTest` (38 unit tests passed), enforcing loopback hosts and disposable database naming conventions, rejecting production, shared, or malformed URLs without credential leakage.
- Teardown: The disposable test database was dropped and confirmed absent from MySQL after execution. Both PR #12 and PR #13 are merged into `main` at commit `296100f` and deployed.

**Frontend:** `node --test frontend/test/*.test.mjs` passed 25 tests with 0 failures. `npm run build` completed successfully. `npm audit --audit-level=moderate` reported 0 known vulnerabilities. `git diff --check` reported no whitespace errors.

To reproduce the frontend checks from the repository root:

```bash
node --test frontend/test/*.test.mjs
cd frontend
npm ci --ignore-scripts
npm run build
npm audit --audit-level=moderate
```

To run the backend suite, use the checked-in Maven wrapper from `backend/`:

```bash
cd backend
./mvnw test
```

For migration and inventory-concurrency validation, set `TEST_DB_URL`, `TEST_DB_USER`, and `TEST_DB_PASSWORD` to a dedicated isolated `resort_test` database only. Never point `TEST_DB_URL` or `DB_URL` at production. The database-backed tests were not run as part of this audit.
---

## 8. Target Cloud Architecture & Railway Port Mechanics

The application uses the following managed-provider topology; availability guarantees and private Railway configuration were not audited:

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
| - Plan: Not independently verified |  | - Auth: Stateless JWT (HS256)   |
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
                                        | - Migrations: Flyway V1-V8      |
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
| **Frontend** | Vercel | `https://chikkamagaluru-resort-booking.vercel.app` | Verified live; deployed from commit `296100f` (`context=Vercel state=success`). Public routes (`/`, `/stay`, `/enquire`, `/staff/login`) return HTTP 200. |
| **Backend** | Railway | `https://resort-backend-production.up.railway.app` | Verified live; running deployment `62741d27-4271-43d2-bc95-0145165c1bd0` built from commit `296100f` (startup verified in container logs at 2026-11-09T04:48:40.424Z / 10:18:40 AM IST, Flyway schema v8 validated, Tomcat port 8080). `GET /health` returns `{"status":"ok"}`. |
| **Database** | Railway MySQL 8 | `mysql.railway.internal:3306/railway` | Persistent Railway volume, schema version v8. Houses confirmed production booking `FE9JZC9DN7PM`. |

> **Release status (9 November 2026)**: The repository `main` is at commit `296100f`. Both PR #12 and PR #13 are merged and deployed.
> - **Confirmed Production Booking**: One live confirmed production booking exists: reference `FE9JZC9DN7PM`. (Guest contact phone number is omitted for privacy). In accordance with pre-submission safeguards, no additional production bookings or inventory writes were performed, and existing reservations were not cancelled or altered.
> - **Check-in / Check-out Presentation Policy**: The displayed 2:00 PM check-in and 11:00 AM checkout times on `frontend/src/pages/BookingConfirmationPage.jsx` and `Styleguide.jsx` originate from frontend UI presentation templates as **demo assumptions**. They have not been confirmed by the resort owner and must not be presented as confirmed resort policy. No production values were modified.
> - **Concurrency & Overbooking Caveat**: Under evaluated MySQL 8.0.45 InnoDB scenarios with `SELECT ... FOR UPDATE` pessimistic row locking, 100 consecutive last-room race iterations, multi-night overlapping ranges, idempotency races, payment/expiry races, and rollback integrity tests passed with zero overselling across the reported 298-test disposable-MySQL run. This demonstrates transaction isolation under tested scenarios, but does not claim an absolute 100% guarantee under unmodeled production failures (e.g. cross-region network partitions or connection pool exhaustion). See [TDD.md](TDD.md) for requirement and audit caveats.

---

## 10. Staff Authentication & Reviewer Access Provisioning

### 10.1 Shared Demo Accounts Decommissioning (Security Hardening)
In response to the security audit finding, all shared public demo accounts have been permanently decommissioned and deactivated from the production environment:
- **Flyway Migration V9**: Added `is_active BOOLEAN NOT NULL DEFAULT TRUE` and deactivated all 4 shared demo accounts (`gm.demo@...`, `rooms.demo@...`, `weddings.demo@...`, `offsites.demo@...` set to `is_active = FALSE`).
- **Frontend Hardening**: Removed all demo account quick-fill buttons and hardcoded credentials from `StaffLoginPage.jsx`. Production bundles contain zero demo credentials.
- **Backend Token & Login Rejection**: `AuthController` rejects authentication for inactive users, and `JwtAuthenticationFilter` validates `is_active` on every authenticated request, rejecting old/replayed JWTs from disabled accounts with HTTP 401.
- **Seeder Safety**: `DemoDataSeeder` is guarded with `isProductionEnvironment()`; it will never seed or reactivate shared demo accounts in cloud/production environments.

### 10.2 Reviewer Access Provisioning (Safe Administrative Action)
Legitimate staff and auditor review access must be provisioned individually with dedicated credentials. To provision reviewer access on the production Railway instance without committing secrets to the repository:

1. **Via Railway Environment Variables (Automated Bootstrap)**:
   In the Railway project dashboard under the backend service **Variables** tab, set:
   - `REVIEWER_EMAIL`: e.g. `reviewer@resort-audit.internal`
   - `REVIEWER_PASSWORD`: `<strong-unique-password>` (e.g. 16+ character random secret)
   - `REVIEWER_NAME`: `Auditor / Reviewer` (optional, default: "Auditor / Reviewer")
   - `REVIEWER_LANE`: `GM_ESCALATION` (optional, default: "GM_ESCALATION")
   - `REVIEWER_IS_GM`: `true` (optional, default: true)

   On container startup, `ReviewerAccountBootstrap` (`@Order(1)`) securely hashes the password using BCrypt and provisions/updates the active reviewer account.

2. **Via Railway MySQL CLI (Direct Admin Action)**:
   If preferred, run this administrative query directly on the production database using the Railway MySQL CLI:
   ```sql
   -- Generate BCrypt hash externally and run:
   INSERT INTO users (email, password_hash, full_name, role, specialty_lane, is_gm, is_active, created_at, updated_at)
   VALUES ('reviewer@resort-audit.internal', '$2a$10$YourBcryptHashHere...', 'Auditor / Reviewer', 'STAFF', 'GM_ESCALATION', 1, 1, NOW(), NOW())
   ON DUPLICATE KEY UPDATE is_active = 1, password_hash = VALUES(password_hash);
   ```

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

## 12. Tax & Hosting Caveats

1. **Provisional GST Slabs (`UNVERIFIED`)**:
   - GST tax calculations applied by the platform (e.g. 5% on tariff <= 7,500 INR, 18% on tariff > 7,500 INR) are provisional and explicitly flagged `UNVERIFIED` in the database.
   - The checkout interface presents a mandatory disclaimer that tax figures are provisional and must be verified by property management with a certified chartered accountant prior to commercial operation.
2. **Hosting-provider plan**:
   - The current Vercel account plan and its applicable terms were not independently verified in this audit. Review the provider's current plan, fair-use rules, and billing requirements before any commercial launch. This guide does not make a legal or plan-compliance determination.
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
curl -s "https://resort-backend-production.up.railway.app/api/v1/public/rooms/availability?checkIn=2026-11-20&checkOut=2026-11-22"

# 4. Verify public pricing calculation (use a future range within the configured booking window)
: "${CHECK_IN:?Export CHECK_IN as a future YYYY-MM-DD date within the configured window}"
: "${CHECK_OUT:?Export CHECK_OUT as a later YYYY-MM-DD date within the configured window}"
curl --get "https://resort-backend-production.up.railway.app/api/v1/public/pricing/quote" \
  --data-urlencode "roomCategoryId=1" \
  --data-urlencode "ratePlanId=1" \
  --data-urlencode "checkIn=${CHECK_IN}" \
  --data-urlencode "checkOut=${CHECK_OUT}"

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
| MySQL-backed integration tests did not run | `TEST_DB_URL` is unset or no dedicated test database is available. | Configure `TEST_DB_URL`, `TEST_DB_USER`, and `TEST_DB_PASSWORD` for an isolated `resort_test` database, then run `./mvnw test` from `backend/`. Never use production credentials or a production database URL. |
| Functional index syntax error on migration `V6` | MySQL version is older than 8.0.16. | Upgrade MySQL Server to version 8.0.16 or newer. Functional indexes and ANSI `CHECK` constraints require MySQL 8.0.16+. |
