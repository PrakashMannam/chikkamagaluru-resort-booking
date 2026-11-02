# Chikkamagaluru Resort Booking

Full-stack resort booking platform: Spring Boot 3 backend + React/Vite frontend.

## Requirements

- **Java 21**
- **Maven 3.9+**
- **Node.js 20+**
- **MySQL 8.0.16 or newer** (required for CHECK constraint enforcement)

## Database Setup

Create the local application database (`resort`) and test database (`resort_test`):

```sql
CREATE DATABASE IF NOT EXISTS resort;
CREATE DATABASE IF NOT EXISTS resort_test;
```

Or via terminal:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS resort; CREATE DATABASE IF NOT EXISTS resort_test;"
```

Flyway will automatically apply migrations (`V1__init.sql`, `V2__schema.sql`, `V3__reference_data.sql`) upon startup or test execution.

## Running the Backend

```bash
cd backend
# Set environment variables (copy .env.example → .env and fill in values, or export them)
export DB_URL=jdbc:mysql://localhost:3306/resort
export DB_USER=resort_user
export DB_PASSWORD=secret
./mvnw spring-boot:run
```

The API starts on **http://localhost:8080**.  
Health check: `GET http://localhost:8080/health` → `{"status":"ok"}`

## Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

The UI starts on **http://localhost:5173**.

## Running Backend Tests

```bash
cd backend
./mvnw test
```

To run the database migration integration test against `resort_test`, set the test database environment variables:

```bash
export TEST_DB_URL=jdbc:mysql://localhost:3306/resort_test
export TEST_DB_USER=resort_user
export TEST_DB_PASSWORD=your_password_here
./mvnw test
```

If `TEST_DB_URL` is not set, the migration integration test is skipped cleanly and unit tests will pass without requiring a database.

## Notes

- Copy `.env.example` to `.env` and fill in real values. **Never commit `.env`.**
- MySQL 8.0.16 or newer is required.
- GST slabs are seeded as `UNVERIFIED` placeholders — verify with a chartered accountant before launch.
