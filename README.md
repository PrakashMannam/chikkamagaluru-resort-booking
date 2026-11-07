# Chikkamagaluru Resort Booking

Full-stack luxury resort booking platform: Spring Boot 3 (Java 21) backend + React 18 / Vite frontend.

For comprehensive architecture, cloud deployment, environment variable matrices, and operational procedures, see [deployment.md](deployment.md).

---

## 1. Prerequisites & Required Software

| Software | Minimum Version | Verified Version | Notes |
| :--- | :--- | :--- | :--- |
| **Java** | OpenJDK 21 LTS | 21.0.11 | Backend runtime (Spring Boot 3.3.5) |
| **Maven** | 3.9+ | 3.9.6 | Or use the Maven wrapper located in `backend/` (`.\backend\mvnw.cmd` or `./backend/mvnw`) |
| **Node.js** | 20+ LTS | 24.14.0 | Frontend toolchain |
| **npm** | 10+ | 10.9.2 | Frontend package manager |
| **MySQL Server** | 8.0.16 or newer | 8.0.36 | Required for ANSI `CHECK` constraints and functional indexes |

---

## 2. Local Database Setup

Create the application database (`resort`) and dedicated test database (`resort_test`):

```sql
CREATE DATABASE IF NOT EXISTS resort CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS resort_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**PowerShell (Windows):**
```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS resort CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE DATABASE IF NOT EXISTS resort_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

**Bash (Linux / macOS):**
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS resort CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE DATABASE IF NOT EXISTS resort_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

> **Flyway Migrations**: Applied automatically on backend startup (`spring.flyway.enabled=true`). Migrations `V1` through `V6` initialize all 12 tables, indexes, constraints, room categories, physical units, rate plans, provisional GST slabs, and blackout uniqueness rules.

---

## 3. Running the Backend

The backend requires 4 mandatory environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`). Spring Boot enforces that `JWT_SECRET` must be at least 32 characters long after trimming.

### Generate a Cryptographically Secure JWT Secret

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

### Start Backend (PowerShell on Windows)

```powershell
cd backend

# Configure mandatory database connection (and JWT_SECRET from above)
$env:DB_URL="jdbc:mysql://localhost:3306/resort?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USER="root"
$env:DB_PASSWORD="your_mysql_password_here"

# Optional configuration
$env:CORS_ALLOWED_ORIGIN="http://localhost:5173"
$env:SPRING_PROFILES_ACTIVE="demo"

# Start the application using backend-local Maven wrapper
.\mvnw.cmd spring-boot:run
```

### Start Backend (Bash on Linux / macOS)

```bash
cd backend

export DB_URL="jdbc:mysql://localhost:3306/resort?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USER="root"
export DB_PASSWORD="your_mysql_password_here"
export CORS_ALLOWED_ORIGIN="http://localhost:5173"
export SPRING_PROFILES_ACTIVE="demo"

./mvnw spring-boot:run
```

- API starts on **http://localhost:8080** (default port).
- Health check: `GET http://localhost:8080/health` → `{"status":"ok"}`.

---

## 4. Running the Frontend

```powershell
cd frontend
npm install
npm run dev
```

- UI starts on **http://localhost:5173**.
- Vite automatically proxies `/api/*` requests to `http://localhost:8080`.

---

## 5. Demo Staff Accounts

With profile `demo` active (`SPRING_PROFILES_ACTIVE=demo`), the following staff accounts are seeded with password `DEMO-ONLY-NOT-REAL-PASS-2026!`:

| Email | Full Name | Role | Specialty Lane | GM Privileges |
| :--- | :--- | :--- | :--- | :--- |
| `gm.demo@companya-estate.test` | Vikram Rao (GM) | `STAFF` | `GM_ESCALATION` | `true` (Full management & escalation) |
| `rooms.demo@companya-estate.test` | Ananya Hegde (Reservations) | `STAFF` | `ROOM` | `false` (Direct bookings, rate quotes & drafts) |
| `weddings.demo@companya-estate.test` | Karthik Urs (Weddings) | `STAFF` | `WEDDING` | `false` (Weddings & banquets) |
| `offsites.demo@companya-estate.test` | Meera Nair (Corporate Offsites) | `STAFF` | `OFFSITE` | `false` (Corporate offsites) |

Staff portal login: **http://localhost:5173/staff/login**.

---

## 6. Running Backend Tests

The backend test suite is divided into fast unit/mock tests and MySQL-backed integration tests. The figures below reflect supplied execution results from the test runner:

### Standard Test Suite (219 Tests — No Database Required)
Runs all 219 Unit, WebMvc, Mock, Rate-Limiter (15 tests), and Service tests (0 failures, 0 errors). Database integration tests skip automatically via JUnit `Assumptions.assumeTrue`:

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

### Full Integration Test Suite (234 Tests — With Local Test Database)
Setting `TEST_DB_URL` activates all 15 database integration tests across `DatabaseMigrationIntegrationTest` (5 tests), `AllotmentConcurrencyIntegrationTest` (6 tests), and `BlackoutConcurrencyIntegrationTest` (4 tests), running all **234 tests** across 26 test classes with 0 failures, 0 errors, and 0 skipped:

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

---

## 7. Cloud Deployment Summary

- **Frontend**: Vercel at `https://chikkamagaluru-resort-booking.vercel.app` (Vercel Hobby plan, non-commercial evaluation only).
- **Backend**: Railway at `https://resort-backend-production.up.railway.app` (Spring Boot 3 on Java 21).
- **Database**: Railway MySQL 8 via private networking.
- **Source & Deployment Status**: Live cloud deployments on Railway and Vercel reflect earlier deployed revision `b4a8308`, based on the last available deployment evidence. The implemented code and documentation remediations (including F-13 public enquiry rate limiting and F-06 checkout progress alignment) are present in the local source through application commit `62ab609` on branch `feature/frontend-completion`, but are **not yet deployed to cloud environments**. Do not assume local improvements are live. The separate Technical Design Document (Manus F-11) is still being prepared and is not yet included in the repository.

Consult [deployment.md](deployment.md) for full deployment details, Railway port mechanics, and troubleshooting.

---

## 8. Essential Notices & Limitations

1. **Security**: Never commit `.env` files or credentials. Use safe placeholders.
2. **Mock Payment Simulator**: Uses internal webhook simulator; no external financial gateway is connected.
3. **Simulated Enquiry Delivery**: Delivery events (`EMAIL_SIM`, `WHATSAPP_SIM`) are logged internally; no external carrier messaging occurs.
4. **Provisional GST Slabs**: Seeded with status `UNVERIFIED`; must be legally verified by a chartered accountant before commercial launch.
5. **Public Enquiry Rate Limiting & Message Boundaries**: `POST /api/v1/public/enquiries` is protected by IP-based rate limiting (5 requests per 10 minutes per IP, configurable via `RATE_LIMIT_ENQUIRY_MAX_REQUESTS`, returning HTTP 429 with `Retry-After: 600`). Raw enquiry messages are strictly bounded to a maximum of 5,000 characters.
6. **Three-Step Booking Journey**: The guest checkout funnel strictly adheres to the three-step booking requirement: Stage 1: Guest Details & Consent (`/checkout`), Stage 2: Payment Guarantee (`/payment/:ref`), Stage 3: Confirmation Receipt (`/bookings/:ref`).
