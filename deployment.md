# Deployment Guide: Chikkamagaluru Resort Booking

This document details the configuration, deployment, and operational procedures for the Chikkamagaluru Resort Booking platform across local development and cloud hosting targets (Vercel, Railway, and MySQL 8).

---

## 1. Local Setup Instructions

### Prerequisites
- **Java**: OpenJDK 21 LTS (`java -version` returns 21.x)
- **Maven**: 3.9+ (`mvn -version`) or use the included `./mvnw` wrapper
- **Node.js**: Node 20+ LTS (`node -v`) and npm 10+ (`npm -v`)
- **MySQL**: MySQL Server 8.0.16 or newer (required for ANSI SQL `CHECK` constraint enforcement)

### Repository Setup
```bash
git clone https://github.com/PrakashMannam/chikkamagaluru-resort-booking.git
cd chikkamagaluru-resort-booking
```

---

## 2. MySQL 8 Database Setup

Create the local application database (`resort`) and the dedicated test database (`resort_test`):

```sql
CREATE DATABASE IF NOT EXISTS resort CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS resort_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Or via terminal:
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS resort; CREATE DATABASE IF NOT EXISTS resort_test;"
```

---

## 3. Flyway Migration Behavior

Database schema versioning is managed strictly via Flyway. The migration files in `backend/src/main/resources/db/migration/` are:

| Migration File | Description |
| :--- | :--- |
| `V1__init.sql` | Baseline placeholder migration (`SELECT 1;`) ensuring Flyway starts cleanly. |
| `V2__schema.sql` | Core schema defining 12 tables (`users`, `room_categories`, `physical_units`, `rate_plans`, `daily_inventory_rates`, `blackout_dates`, `promo_codes`, `gst_tax_slabs`, `bookings`, `payments`, `enquiries`, `enquiry_drafts`) with foreign keys, uniqueness constraints, and ANSI `CHECK` constraints. |
| `V3__reference_data.sql` | Static reference data: 2 room categories (`ROOM`, `VILLA`), 76 physical units (`R-101`..`R-164`, `V-01`..`V-12`), 3 rate plans per category (`EP`, `CP`, `MAP`), initial blackout dates (`2026-12-24`, `2026-12-25`), and active promo codes (`DIRECT10`, `COFFEE1500`). |
| `V4__gst_slabs.sql` | Realistic provisional GST tax slabs (0.00–7,500.00 INR: 5.00%, >7,500.00 INR: 18.00%, status `UNVERIFIED`). |
| `V5__enquiry_ai_fields.sql` | Schema enhancement adding `ai_summary` (TEXT), `extracted_rooms` (INT), and `ai_error_status` (VARCHAR(100)) to the `enquiries` table. |

- **Startup Execution**: When `spring.flyway.enabled=true`, migrations execute automatically on application startup prior to Hibernate schema validation (`spring.jpa.hibernate.ddl-auto=validate`).
- **Immutability**: Applied migration scripts (`V1`–`V5`) are immutable and never modified in place.

---

## 4. Backend Execution

### Local Configuration
Create a local `.env` file in the root directory (never commit `.env`):
```properties
DB_URL=jdbc:mysql://localhost:3306/resort?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USER=root
DB_PASSWORD=your_password
JWT_SECRET=your_minimum_32_char_strong_random_secret_here
CORS_ALLOWED_ORIGIN=http://localhost:5173
```

### Run Command (Demo Mode)
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```
The API starts on `http://localhost:8080`.
Health check endpoint: `GET http://localhost:8080/health` → `{"status":"ok"}`.

---

## 5. Frontend Execution

```bash
cd frontend
npm install
npm run dev
```
The frontend dev server starts on `http://localhost:5173`.

---

## 6. Required Environment Variable Names

Configure the following environment variable **NAMES** in the respective environment (Railway / Vercel / Local). Never commit secret values to source repositories.

### Backend (Railway / Container Environment)
| Variable Name | Required | Description |
| :--- | :--- | :--- |
| `DB_URL` | Yes | JDBC connection string to persistent MySQL (e.g. `jdbc:mysql://${MYSQLHOST}:${MYSQLPORT}/${MYSQLDATABASE}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`) |
| `DB_USER` | Yes | MySQL database username |
| `DB_PASSWORD` | Yes | MySQL database password |
| `JWT_SECRET` | Yes | Cryptographic secret for signing staff authentication tokens (min 256-bit / 32 characters) |
| `SPRING_PROFILES_ACTIVE` | Recommended | Set to `demo` to activate `DemoDataSeeder` for staff reviewer logins and seed rates |
| `CORS_ALLOWED_ORIGIN` | Yes | URL of deployed frontend (e.g. `https://<your-app>.vercel.app`) |
| `SERVER_PORT` / `PORT` | Optional | Dynamic server port. Spring Boot binds to `server.port=${PORT:8080}` via JVM arg `-Dserver.port=${PORT:8080}` or `SERVER_PORT=${PORT}` |
| `FIXED_TODAY` | Optional | Fixed reference date for demo testing (e.g. `2026-11-02`) |
| `RATE_LIMIT_BOOKING_RPM` | Optional | Maximum booking attempts per IP per minute (default `10`) |
| `TRUSTED_PROXY_HOPS` | Optional | Number of upstream reverse-proxy hops for client IP extraction (default `1`) |
| `GEMINI_API_KEY` | Optional | Google Gemini API key for automated enquiry triage. If omitted, `RuleFallbackService` operates automatically |
| `GEMINI_MODEL` | Optional | Gemini model identifier (default `gemini-3.8-flash`) |
| `ENQUIRY_CONFIDENCE_THRESHOLD` | Optional | Confidence threshold for automated draft triage (default `0.80`) |

### Local Testing Environment Only
| Variable Name | Required | Description |
| :--- | :--- | :--- |
| `TEST_DB_URL` | Optional (Required for DB tests) | JDBC URL to local `resort_test` database (e.g. `jdbc:mysql://localhost:3306/resort_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`) |
| `TEST_DB_USER` | Optional (Required for DB tests) | MySQL test database user (default `root`) |
| `TEST_DB_PASSWORD` | Optional (Required for DB tests) | MySQL test database password |

> **Note on Test Suite Coverage**:
> - **Standard Suite (155 tests)**: Executing `mvn test` without `TEST_DB_URL` set runs all 155 Unit, Service, WebMvc, and Security test cases. The two database-backed integration test classes (`DatabaseMigrationIntegrationTest` and `AllotmentConcurrencyIntegrationTest`) automatically abort during `@BeforeAll` via JUnit `Assumptions.assumeTrue`, counting 0 tests executed for those classes.
> - **Full Integration Suite (165 tests)**: When `TEST_DB_URL` is set pointing to the local `resort_test` database, Surefire executes all 165 tests (155 standard tests + 5 Flyway migration tests in `DatabaseMigrationIntegrationTest` + 5 thread concurrency tests in `AllotmentConcurrencyIntegrationTest`) with 0 failures, 0 errors, and 0 skipped.

### Frontend (Vercel)
| Variable Name | Required | Description |
| :--- | :--- | :--- |
| `VITE_API_URL` | Yes | Public HTTPS base URL of the deployed Railway backend (e.g. `https://<backend-app>.up.railway.app`) |

---

## 7. Railway MySQL Database Provisioning (Target Cloud Architecture)

1. Log in to [Railway](https://railway.app).
2. Create a new project or select an existing workspace.
3. Click **+ New** → **Database** → **Add MySQL**.
4. Railway provisions a persistent MySQL 8 instance with an attached volume.
5. In MySQL service settings, copy the connection variables:
   - `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE`.
6. Construct the JDBC URL:
   `jdbc:mysql://${MYSQLHOST}:${MYSQLPORT}/${MYSQLDATABASE}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`
7. Ensure volume persistence is enabled for durable data storage across service restarts.

---

## 8. Railway Backend Deployment (Target Cloud Architecture)

1. Set the project root to `backend/` or configure root build commands.
2. Build & Startup Configuration:
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -Dserver.port=${PORT:8080} -jar target/resort-backend-0.0.1-SNAPSHOT.jar`
   - Dynamic port handling: Railway injects `PORT`. Passing `-Dserver.port=${PORT:8080}` ensures Spring Boot binds dynamically.
3. Environment Variables (set via Railway dashboard):
   - `DB_URL`, `DB_USER`, `DB_PASSWORD` (reference the Railway MySQL service).
   - `JWT_SECRET` (generate a strong random 256-bit secret).
   - `SPRING_PROFILES_ACTIVE=demo` (enables demo review accounts and inventory).
   - `CORS_ALLOWED_ORIGIN=https://<your-vercel-domain>.vercel.app`.
4. Deploy the service and verify health:
   `curl https://<backend-domain>.up.railway.app/health` → `{"status":"ok"}`.

---

## 9. Vercel Frontend Deployment (Target Cloud Architecture)

1. Deploy via Vercel CLI or link GitHub repository in [Vercel Dashboard](https://vercel.com):
   - **Root Directory**: `frontend`
   - **Framework Preset**: Vite
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
2. Environment Variables:
   - Set `VITE_API_URL` to `https://<backend-domain>.up.railway.app`
3. Single Page Application (SPA) Routing:
   `frontend/vercel.json` contains the essential SPA rewrite configuration:
   ```json
   {
     "rewrites": [
       { "source": "/(.*)", "destination": "/index.html" }
     ]
   }
   ```
   This ensures direct browser navigation and page reloads for routes (`/stay`, `/book/checkout`, `/staff/login`, `/staff/enquiries`) resolve correctly to `index.html`.

---

## 10. Deployment Status & Live Infrastructure URLs

### Current Deployment Status
- **Backend Service (Railway)**: **CONFIRMED LIVE & HEALTHY**
  - **Public HTTPS API URL**: `https://resort-backend-production.up.railway.app`
  - **Public Health Check URL**: `https://resort-backend-production.up.railway.app/health` → `{"status":"ok"}` (HTTP 200)
  - **Railway Account**: `prakashmannam9@gmail.com`
  - **Railway Project ID**: `737ac4cb-6069-43c7-a658-95c6fed8d092` (`chikkamagaluru-resort-booking`)
  - **Railway Service ID**: `1be9fd4f-6cf1-4b1a-8b14-7b31d00129db` (`resort-backend`)
  - **Active Deployment ID**: `077e3a9d-f98c-42d0-b0cf-7daeddcc6ae4`
  - **Hosting Plan & Cost Observation**: Deployed using trial/usage-based credits observed during deployment verification (~$5.00 credit balance). This is recorded strictly as an observation from the deployment inspection, not a promise or guarantee that future hosting will remain free or uninterrupted once trial credits or quotas are consumed.
- **Database Service (Railway MySQL 8)**: **CONFIRMED LIVE & PERSISTENT**
  - **Service Name / ID**: `MySQL` (`3752b22a-eb66-46b2-9503-9c52bb4479dd`)
  - **Storage**: Attached persistent volume `mysql-volume` mounted at `/var/lib/mysql`
  - **Internal Network**: Private networking via `mysql.railway.internal:3306/railway`
  - **Flyway Migration Status**: **V1 through V5 successfully applied**; schema validated at version `v5`.
  - **Seeded Demo State**: 2 room categories (`ROOM`, `VILLA`), 76 physical units, 6 rate plans, demo inventory rates, 4 staff accounts, provisional GST slabs (`UNVERIFIED`).
- **Frontend Service (Vercel)**: **CONFIRMED LIVE & PUBLICLY VERIFIED**
  - **Live Production URL**: `https://chikkamagaluru-resort-booking.vercel.app`
  - **Direct Deployment URL**: `https://chikkamagaluru-resort-booking-1jwgbxmte-prakash-s-projects22.vercel.app`
  - **Vercel Project ID**: `prj_UnfZoFQJonqeqeDrsqZK2PUkBu29` (`chikkamagaluru-resort-booking`)
  - **Account / Owner**: `prakash-s-projects22` (`prakashmannam8-8753`)
  - **Vercel Plan**: `Hobby` (Personal account tier)
  - **Production Environment Variables**: `VITE_API_URL=https://resort-backend-production.up.railway.app`
  - **CORS Allowed Origin**: `https://chikkamagaluru-resort-booking.vercel.app` (configured on Railway backend and verified via OPTIONS preflight)
  - **SPA Routing**: Verified via `frontend/vercel.json` rewrites; direct browser navigation and reloads across `/stay`, `/checkout`, `/staff/login`, `/staff/enquiries`, and `/staff/inventory` resolve without 404s.
  - **Asset Delivery**: All images (including auxiliary images `coffee-cherries.jpg`, `coffee-villa.jpg`, `corporate-dining.jpg`, `hills-panoramic.jpg`, `wedding-lawn.jpg`) verified loading with HTTP 200 from Vercel edge CDN.

---

## 11. Demo Login Accounts

When running with `SPRING_PROFILES_ACTIVE=demo`, the following accounts are initialized by `DemoDataSeeder.java`:

**Common Demo Password**:
```text
DEMO-ONLY-NOT-REAL-PASS-2026!
```

| Email | Full Name | Role | Specialty Lane | Scope / Permissions |
| :--- | :--- | :--- | :--- | :--- |
| `rooms.demo@companya-estate.test` | Ananya Hegde (Reservations) | `STAFF` | `ROOM` | Front Office reservations, rate quotes, draft approval, and enquiry simulator |
| `gm.demo@companya-estate.test` | Vikram Rao (GM) | `STAFF` | `GM_ESCALATION` (`is_gm=true`) | Full management access, GM alert banner, guest complaints queue (`COMPLAINT`), and escalation approval |
| `weddings.demo@companya-estate.test` | Karthik Urs (Weddings) | `STAFF` | `WEDDING` | Wedding and banquet lawn enquiries |
| `offsites.demo@companya-estate.test` | Meera Nair (Corporate Offsites) | `STAFF` | `OFFSITE` | Corporate offsite and retreat enquiries |

---

## 12. Mock Payment & Simulated Enquiry Delivery Limitations

1. **Mock Payment Simulator**:
   - Payment simulation operates via `POST /api/v1/public/payments/{bookingRef}/mock-webhook`.
   - No real financial payment gateways, card networks, or UPI transfers are connected.
   - Payment tokens are single-use (`one_time_payment_token`) and invalidated immediately upon transaction completion or hold cancellation.
2. **Simulated Enquiry Dispatch**:
   - The staff enquiry simulator (`POST /api/v1/admin/enquiries/simulate`) supports demonstration channels `EMAIL_SIM` and `WHATSAPP_SIM`.
   - Approving drafts marks them as `SENT_MANUALLY` with simulated dispatch timestamps.
   - No real outbound SMS, WhatsApp messages, or emails are transmitted over external carrier networks.

---

## 13. Known Limitations & Tax Compliance

1. **Provisional GST Slabs (`UNVERIFIED`)**:
   - GST tax calculations applied by the platform (e.g. 18% for tariff brackets above 7,500 INR) are provisional and labeled `UNVERIFIED`.
   - The guest checkout and payment interfaces display the explicit disclaimer:
     > *"GST rates are provisional and pending verification. Final tax treatment must be confirmed by the property."*
   - Property management must verify tax rates and HSN codes with a certified chartered accountant prior to commercial billing.
2. **Booking Cancellation Boundary**:
   - The direct guest booking interface provides `cancelHold` for bookings in `PENDING_PAYMENT` status prior to payment.
   - Confirmed bookings (`CONFIRMED`) cannot be cancelled through the guest self-service checkout workflow and require staff coordination.
3. **Vercel Hobby Plan & Non-Commercial Demonstration Limitation**:
   - This Vercel deployment is hosted under the personal **Hobby** plan and is intended strictly for non-commercial take-home evaluation, developer portfolio demonstration, and technical assessment.
   - It must **not** be used to advertise or operate actual resort sales, commercial bookings, or real payment collection under the Hobby plan.
   - Commercial operation requires upgrading to a Vercel plan that permits commercial use, such as **Pro** or **Enterprise**.
   - The payment flow in this application is entirely simulated via mock payment webhooks; no real financial transactions take place.
   - Official Vercel policy references:
     - [Vercel Hobby Plan](https://vercel.com/docs/plans/hobby)
     - [Vercel Fair Use Guidelines](https://vercel.com/docs/limits/fair-use-guidelines)
     - [Vercel Terms of Service](https://vercel.com/legal/terms)

---

## 14. Static Image Asset Packaging

The application utilizes image assets located in `frontend/public/images/`:
- **Tracked Assets (Required by Application Code)**:
  `hero-estate.webp`, `hero-estate-800w.webp`, `room-canopy.webp`, `room-canopy-1600w.webp`, `villa-pool.webp`, `villa-pool-1600w.webp`, `coffee-plantation-cherries.jpg`, `wedding-lawn-estate.jpg`, and `corporate-offsite-estate.jpg` (along with vector/raster fallbacks). These files are directly referenced in `LandingPage.jsx` and other UI components and are tracked in Git.
- **Untracked Local Assets (Unreferenced Auxiliary Images)**:
  Five auxiliary images reside in `frontend/public/images/`: `coffee-cherries.jpg`, `coffee-villa.jpg`, `corporate-dining.jpg`, `hills-panoramic.jpg`, and `wedding-lawn.jpg`. An audit confirms that none of these five files are referenced by application source code, stylesheets, or markup.
- **Packaging Guarantee & Reproducibility**:
  During `npm run build`, Vite automatically copies the entire `frontend/public/` directory into `dist/`. All image assets in `frontend/public/images/` were bundled into `dist/images/` and served correctly in the live Vercel deployment. However, reproducible builds of the application code do not depend on the five untracked images.

---

## 15. Verification & Health Audit Commands

To verify an active deployment:
```bash
# Verify backend health
curl -s -i https://<backend-domain>/health

# Verify public pricing calculation
curl -s "https://<backend-domain>/api/v1/public/pricing/quote?roomCategoryId=1&ratePlanId=1&checkIn=2026-11-13&checkOut=2026-11-16"

# Verify frontend SPA load
curl -s -i https://<frontend-domain>/
```
