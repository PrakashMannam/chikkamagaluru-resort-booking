# Chikkamagaluru Resort Booking

Full-stack resort booking platform: Spring Boot 3 backend + React/Vite frontend.

## Requirements

- **Java 21**
- **Maven 3.9+**
- **Node.js 20+**
- **MySQL 8.0.16 or newer**

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

## Notes

- Copy `.env.example` to `.env` and fill in real values. **Never commit `.env`.**
- MySQL 8.0.16 or newer is required.
- GST slabs are seeded as `UNVERIFIED` placeholders — verify with a chartered accountant before launch.
