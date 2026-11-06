package com.companya.resort;

import com.companya.resort.dto.BookingResponseDto;
import com.companya.resort.dto.CreateBookingRequest;
import com.companya.resort.entity.*;
import com.companya.resort.exception.InsufficientAllotmentException;
import com.companya.resort.repository.*;
import com.companya.resort.service.BookingService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllotmentConcurrencyIntegrationTest {

    private static String testDbUrl;
    private static String testDbUser;
    private static String testDbPassword;
    private static ConfigurableApplicationContext applicationContext;
    private static BookingService bookingService;
    private static com.companya.resort.service.BookingPaymentService bookingPaymentService;
    private static com.companya.resort.service.BookingExpiryService bookingExpiryService;
    private static BookingRepository bookingRepository;
    private static PaymentRepository paymentRepository;
    private static RoomCategoryRepository roomCategoryRepository;
    private static RatePlanRepository ratePlanRepository;
    private static PromoCodeRepository promoCodeRepository;
    private static Clock clock;

    @BeforeAll
    static void setUp() {
        testDbUrl = System.getenv("TEST_DB_URL");
        testDbUser = System.getenv("TEST_DB_USER");
        testDbPassword = System.getenv("TEST_DB_PASSWORD");

        Assumptions.assumeTrue(
                testDbUrl != null && !testDbUrl.trim().isEmpty(),
                "Skipping concurrency integration test: TEST_DB_URL environment variable is not set."
        );

        if (testDbUser == null) {
            testDbUser = "root";
        }
        if (testDbPassword == null) {
            testDbPassword = "root";
        }

        Flyway flyway = Flyway.configure()
                .dataSource(testDbUrl, testDbUser, testDbPassword)
                .load();
        flyway.migrate();

        applicationContext = new SpringApplicationBuilder(ResortApplication.class)
                .web(org.springframework.boot.WebApplicationType.NONE)
                .properties(
                        "DB_URL=" + testDbUrl,
                        "DB_USER=" + testDbUser,
                        "DB_PASSWORD=" + testDbPassword,
                        "spring.datasource.url=" + testDbUrl,
                        "spring.datasource.username=" + testDbUser,
                        "spring.datasource.password=" + testDbPassword,
                        "spring.jpa.hibernate.ddl-auto=none",
                        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
                        "spring.flyway.enabled=false",
                        "JWT_SECRET=12345678901234567890123456789012",
                        "app.jwt.secret=12345678901234567890123456789012",
                        "app.booking.fixed-today=2026-11-02"
                )
                .run();

        bookingService = applicationContext.getBean(BookingService.class);
        bookingPaymentService = applicationContext.getBean(com.companya.resort.service.BookingPaymentService.class);
        bookingExpiryService = applicationContext.getBean(com.companya.resort.service.BookingExpiryService.class);
        bookingRepository = applicationContext.getBean(BookingRepository.class);
        paymentRepository = applicationContext.getBean(PaymentRepository.class);
        roomCategoryRepository = applicationContext.getBean(RoomCategoryRepository.class);
        ratePlanRepository = applicationContext.getBean(RatePlanRepository.class);
        promoCodeRepository = applicationContext.getBean(PromoCodeRepository.class);
        clock = applicationContext.getBean(Clock.class);
    }

    @AfterAll
    static void tearDown() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(testDbUrl, testDbUser, testDbPassword);
        conn.setAutoCommit(false);
        return conn;
    }

    @Test
    void when20ThreadsBookLast2VillasSimultaneously_exact2SucceedAnd18Fail() throws Exception {
        Long villaCategoryId = 2L;
        LocalDate date1 = LocalDate.of(2026, 11, 14);
        LocalDate date2 = LocalDate.of(2026, 11, 15);

        // Reset/seed allotment in test database: 2 nights with website_allotment_total = 2, booked_count = 0
        try (Connection conn = getConnection()) {
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM daily_inventory_rates WHERE room_category_id = ? AND stay_date IN (?, ?)")) {
                deleteStmt.setLong(1, villaCategoryId);
                deleteStmt.setDate(2, Date.valueOf(date1));
                deleteStmt.setDate(3, Date.valueOf(date2));
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO daily_inventory_rates (room_category_id, stay_date, base_rate_inr, website_allotment_total, booked_count, version) VALUES (?, ?, ?, ?, ?, ?)")) {
                insertStmt.setLong(1, villaCategoryId);
                insertStmt.setDate(2, Date.valueOf(date1));
                insertStmt.setBigDecimal(3, new BigDecimal("18000.00"));
                insertStmt.setInt(4, 2);
                insertStmt.setInt(5, 0);
                insertStmt.setLong(6, 0L);
                insertStmt.executeUpdate();

                insertStmt.setDate(2, Date.valueOf(date2));
                insertStmt.executeUpdate();
            }
            conn.commit();
        }

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                readyLatch.countDown();
                try {
                    startGun.await(); // Simultaneous trigger
                    try (Connection conn = getConnection()) {
                        conn.setAutoCommit(false);
                        // Pessimistic write lock in ascending date order
                        try (PreparedStatement selectStmt = conn.prepareStatement(
                                "SELECT id, website_allotment_total, booked_count FROM daily_inventory_rates " +
                                        "WHERE room_category_id = ? AND stay_date >= ? AND stay_date < ? " +
                                        "ORDER BY stay_date ASC FOR UPDATE")) {
                            selectStmt.setLong(1, villaCategoryId);
                            selectStmt.setDate(2, Date.valueOf(date1));
                            selectStmt.setDate(3, Date.valueOf(date2.plusDays(1)));

                            boolean canBook = true;
                            try (ResultSet rs = selectStmt.executeQuery()) {
                                int rowCount = 0;
                                while (rs.next()) {
                                    rowCount++;
                                    int allotment = rs.getInt("website_allotment_total");
                                    int booked = rs.getInt("booked_count");
                                    if (booked + 1 > allotment) {
                                        canBook = false;
                                    }
                                }
                                if (rowCount != 2) {
                                    canBook = false;
                                }
                            }

                            if (canBook) {
                                try (PreparedStatement updateStmt = conn.prepareStatement(
                                        "UPDATE daily_inventory_rates SET booked_count = booked_count + 1 " +
                                                "WHERE room_category_id = ? AND stay_date >= ? AND stay_date < ?")) {
                                    updateStmt.setLong(1, villaCategoryId);
                                    updateStmt.setDate(2, Date.valueOf(date1));
                                    updateStmt.setDate(3, Date.valueOf(date2.plusDays(1)));
                                    updateStmt.executeUpdate();
                                }
                                conn.commit();
                                successCount.incrementAndGet();
                            } else {
                                conn.rollback();
                                rejectedCount.incrementAndGet();
                            }
                        }
                    }
                } catch (Exception ex) {
                    rejectedCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startGun.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(2, successCount.get(), "Exactly 2 concurrent reservations must succeed for 2 available units");
        assertEquals(18, rejectedCount.get(), "Exactly 18 concurrent reservations must be rejected when allotment is exhausted");

        // Verify database row state
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(
                     "SELECT booked_count FROM daily_inventory_rates WHERE room_category_id = ? AND stay_date IN (?, ?)")) {
            checkStmt.setLong(1, villaCategoryId);
            checkStmt.setDate(2, Date.valueOf(date1));
            checkStmt.setDate(3, Date.valueOf(date2));
            try (ResultSet rs = checkStmt.executeQuery()) {
                while (rs.next()) {
                    assertEquals(2, rs.getInt(1), "Booked count in DB must equal exactly 2 (never > allotment)");
                }
            }
        }
    }

    @Test
    void idempotencyRace_twoThreadsSameKeyAndPayload_bothGetSameBookingRefAndBookedCountRisesOnce() throws Exception {
        Long villaCategoryId = 2L;
        Long ratePlanId = 4L; // Villa EP plan from V3 reference data
        LocalDate d1 = LocalDate.of(2026, 11, 20);
        LocalDate d2 = LocalDate.of(2026, 11, 21);
        String idempotencyKey = "race-idem-" + System.currentTimeMillis();
        String guestEmail = "racer_" + System.currentTimeMillis() + "@example.com";
        String guestPhone = "98" + String.format("%08d", Math.abs(System.currentTimeMillis() % 100000000L));

        // Prepare inventory and clean up any past test data
        try (Connection conn = getConnection()) {
            try (PreparedStatement cleanPayments = conn.prepareStatement(
                    "DELETE FROM payments WHERE booking_id IN (SELECT id FROM bookings WHERE idempotency_key = ? OR guest_email = ?)")) {
                cleanPayments.setString(1, idempotencyKey);
                cleanPayments.setString(2, guestEmail);
                cleanPayments.executeUpdate();
            }
            try (PreparedStatement cleanBookings = conn.prepareStatement(
                    "DELETE FROM bookings WHERE idempotency_key = ? OR guest_email = ?")) {
                cleanBookings.setString(1, idempotencyKey);
                cleanBookings.setString(2, guestEmail);
                cleanBookings.executeUpdate();
            }
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM daily_inventory_rates WHERE room_category_id = ? AND stay_date IN (?, ?)")) {
                deleteStmt.setLong(1, villaCategoryId);
                deleteStmt.setDate(2, Date.valueOf(d1));
                deleteStmt.setDate(3, Date.valueOf(d2));
                deleteStmt.executeUpdate();
            }
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO daily_inventory_rates (room_category_id, stay_date, base_rate_inr, website_allotment_total, booked_count, version) VALUES (?, ?, ?, ?, ?, ?)")) {
                insertStmt.setLong(1, villaCategoryId);
                insertStmt.setDate(2, Date.valueOf(d1));
                insertStmt.setBigDecimal(3, new BigDecimal("16500.00"));
                insertStmt.setInt(4, 5);
                insertStmt.setInt(5, 0);
                insertStmt.setLong(6, 0L);
                insertStmt.executeUpdate();

                insertStmt.setDate(2, Date.valueOf(d2));
                insertStmt.executeUpdate();
            }
            conn.commit();
        }

        CreateBookingRequest request = new CreateBookingRequest(
                idempotencyKey, villaCategoryId, ratePlanId, d1, d2.plusDays(1), 1, 2, 0,
                "Racer Guest", guestEmail, guestPhone, null, true
        );

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGun = new CountDownLatch(1);

        Future<BookingResponseDto> f1 = pool.submit(() -> {
            startGun.await();
            return bookingService.createBookingAndLockAllotment(request, idempotencyKey);
        });
        Future<BookingResponseDto> f2 = pool.submit(() -> {
            startGun.await();
            return bookingService.createBookingAndLockAllotment(request, idempotencyKey);
        });

        startGun.countDown();

        BookingResponseDto r1 = f1.get(15, TimeUnit.SECONDS);
        BookingResponseDto r2 = f2.get(15, TimeUnit.SECONDS);
        pool.shutdown();

        assertNotNull(r1);
        assertNotNull(r2);
        assertEquals(r1.getBookingRef(), r2.getBookingRef(), "Both threads must receive the identical bookingRef");
        assertEquals(idempotencyKey, r1.getIdempotencyKey());
        assertEquals(idempotencyKey, r2.getIdempotencyKey());

        // Verify that booked_count in DB only rose once (from 0 to 1)
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(
                     "SELECT booked_count FROM daily_inventory_rates WHERE room_category_id = ? AND stay_date IN (?, ?)")) {
            checkStmt.setLong(1, villaCategoryId);
            checkStmt.setDate(2, Date.valueOf(d1));
            checkStmt.setDate(3, Date.valueOf(d2));
            try (ResultSet rs = checkStmt.executeQuery()) {
                while (rs.next()) {
                    assertEquals(1, rs.getInt("booked_count"), "Booked count must have risen exactly once");
                }
            }
        }

        // Verify exactly one booking row exists in bookings table
        try (Connection conn = getConnection();
             PreparedStatement checkBookingStmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM bookings WHERE idempotency_key = ?")) {
            checkBookingStmt.setString(1, idempotencyKey);
            try (ResultSet rs = checkBookingStmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Exactly one booking row must exist for this idempotency key");
            }
        }
    }

    @Test
    void overlappingRangeConcurrency_10Threads14to16And10Threads15to17_noDateExceedsAllotment() throws Exception {
        Long villaCategoryId = 2L;
        Long ratePlanId = 4L; // Villa EP plan
        LocalDate d14 = LocalDate.of(2026, 11, 14);
        LocalDate d15 = LocalDate.of(2026, 11, 15);
        LocalDate d16 = LocalDate.of(2026, 11, 16);

        // Reset/seed allotment 2 on each of 14, 15 and 16 Nov
        try (Connection conn = getConnection()) {
            try (PreparedStatement cleanPayments = conn.prepareStatement(
                    "DELETE FROM payments WHERE booking_id IN (SELECT id FROM bookings WHERE idempotency_key LIKE 'range-test-%')")) {
                cleanPayments.executeUpdate();
            }
            try (PreparedStatement cleanBookings = conn.prepareStatement(
                    "DELETE FROM bookings WHERE idempotency_key LIKE 'range-test-%'")) {
                cleanBookings.executeUpdate();
            }
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM daily_inventory_rates WHERE room_category_id = ? AND stay_date IN (?, ?, ?)")) {
                deleteStmt.setLong(1, villaCategoryId);
                deleteStmt.setDate(2, Date.valueOf(d14));
                deleteStmt.setDate(3, Date.valueOf(d15));
                deleteStmt.setDate(4, Date.valueOf(d16));
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO daily_inventory_rates (room_category_id, stay_date, base_rate_inr, website_allotment_total, booked_count, version) VALUES (?, ?, ?, ?, ?, ?)")) {
                for (LocalDate d : List.of(d14, d15, d16)) {
                    insertStmt.setLong(1, villaCategoryId);
                    insertStmt.setDate(2, Date.valueOf(d));
                    insertStmt.setBigDecimal(3, new BigDecimal("18000.00"));
                    insertStmt.setInt(4, 2); // Allotment 2
                    insertStmt.setInt(5, 0); // Booked 0
                    insertStmt.setLong(6, 0L);
                    insertStmt.executeUpdate();
                }
            }
            conn.commit();
        }

        int threadsGroupA = 10;
        int threadsGroupB = 10;
        int totalThreads = threadsGroupA + threadsGroupB;

        ExecutorService pool = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        List<Throwable> unexpectedExceptions = new CopyOnWriteArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientCount = new AtomicInteger(0);

        long uniqueRunId = System.currentTimeMillis();

        // 10 threads book 14-16 Nov (nights: 14 and 15 Nov)
        for (int i = 0; i < threadsGroupA; i++) {
            final int idx = i;
            pool.submit(() -> {
                readyLatch.countDown();
                try {
                    startGun.await();
                    String key = "range-test-14-" + uniqueRunId + "-" + idx;
                    CreateBookingRequest request = new CreateBookingRequest(
                            key, villaCategoryId, ratePlanId,
                            d14, LocalDate.of(2026, 11, 16),
                            1, 2, 0,
                            "Guest A " + idx, "guesta" + idx + "_" + uniqueRunId + "@test.com", "9871" + String.format("%06d", idx), null, true
                    );
                    bookingService.createBookingAndLockAllotment(request, key);
                    successCount.incrementAndGet();
                } catch (InsufficientAllotmentException ex) {
                    insufficientCount.incrementAndGet();
                } catch (Throwable t) {
                    unexpectedExceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 10 threads book 15-17 Nov (nights: 15 and 16 Nov)
        for (int i = 0; i < threadsGroupB; i++) {
            final int idx = i;
            pool.submit(() -> {
                readyLatch.countDown();
                try {
                    startGun.await();
                    String key = "range-test-15-" + uniqueRunId + "-" + idx;
                    CreateBookingRequest request = new CreateBookingRequest(
                            key, villaCategoryId, ratePlanId,
                            d15, LocalDate.of(2026, 11, 17),
                            1, 2, 0,
                            "Guest B " + idx, "guestb" + idx + "_" + uniqueRunId + "@test.com", "9872" + String.format("%06d", idx), null, true
                    );
                    bookingService.createBookingAndLockAllotment(request, key);
                    successCount.incrementAndGet();
                } catch (InsufficientAllotmentException ex) {
                    insufficientCount.incrementAndGet();
                } catch (Throwable t) {
                    unexpectedExceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startGun.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All booking threads must complete within timeout");
        pool.shutdown();

        // Collect unexpected exceptions into a list and assert it is empty
        assertTrue(unexpectedExceptions.isEmpty(), "Unexpected exceptions during overlapping concurrency: " + unexpectedExceptions);

        // Verify that no date exceeds its allotment in the database
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(
                     "SELECT stay_date, website_allotment_total, booked_count FROM daily_inventory_rates " +
                             "WHERE room_category_id = ? AND stay_date IN (?, ?, ?)")) {
            checkStmt.setLong(1, villaCategoryId);
            checkStmt.setDate(2, Date.valueOf(d14));
            checkStmt.setDate(3, Date.valueOf(d15));
            checkStmt.setDate(4, Date.valueOf(d16));
            try (ResultSet rs = checkStmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate stayDate = rs.getDate("stay_date").toLocalDate();
                    int allotment = rs.getInt("website_allotment_total");
                    int booked = rs.getInt("booked_count");
                    assertTrue(booked <= allotment,
                            "Date " + stayDate + " booked_count (" + booked + ") must never exceed allotment (" + allotment + ")");
                }
            }
        }
    }

    @Test
    void webhookAndExpiryJobRacingReleaseStockExactlyOnce_databaseIntegrationTest() throws Exception {
        Long roomCategoryId = 1L;
        LocalDate d1 = LocalDate.of(2026, 12, 10);
        LocalDate d2 = LocalDate.of(2026, 12, 11);
        String bookingRef = "RACE12345678";
        String idempotencyKey = "race-idem-" + System.currentTimeMillis();
        String paymentToken = "race_token_" + System.currentTimeMillis();

        // Prepare inventory with booked_count = 1
        try (Connection conn = getConnection()) {
            try (PreparedStatement clean = conn.prepareStatement(
                    "DELETE FROM payments WHERE one_time_payment_token = ? OR booking_id IN (SELECT id FROM bookings WHERE booking_ref = ? OR idempotency_key = ?)")) {
                clean.setString(1, paymentToken);
                clean.setString(2, bookingRef);
                clean.setString(3, idempotencyKey);
                clean.executeUpdate();
            }
            try (PreparedStatement clean = conn.prepareStatement(
                    "DELETE FROM bookings WHERE booking_ref = ? OR idempotency_key = ?")) {
                clean.setString(1, bookingRef);
                clean.setString(2, idempotencyKey);
                clean.executeUpdate();
            }
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM daily_inventory_rates WHERE room_category_id = ? AND stay_date IN (?, ?)")) {
                deleteStmt.setLong(1, roomCategoryId);
                deleteStmt.setDate(2, Date.valueOf(d1));
                deleteStmt.setDate(3, Date.valueOf(d2));
                deleteStmt.executeUpdate();
            }
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO daily_inventory_rates (room_category_id, stay_date, base_rate_inr, website_allotment_total, booked_count, version) VALUES (?, ?, ?, ?, ?, ?)")) {
                insertStmt.setLong(1, roomCategoryId);
                insertStmt.setDate(2, Date.valueOf(d1));
                insertStmt.setBigDecimal(3, new BigDecimal("8500.00"));
                insertStmt.setInt(4, 5);
                insertStmt.setInt(5, 1); // 1 unit already booked
                insertStmt.setLong(6, 0L);
                insertStmt.executeUpdate();

                insertStmt.setLong(1, roomCategoryId);
                insertStmt.setDate(2, Date.valueOf(d2));
                insertStmt.setBigDecimal(3, new BigDecimal("8500.00"));
                insertStmt.setInt(4, 5);
                insertStmt.setInt(5, 1); // 1 unit already booked
                insertStmt.setLong(6, 0L);
                insertStmt.executeUpdate();
            }

            // Insert expired booking and payment row
            long bookingId;
            try (PreparedStatement insertBooking = conn.prepareStatement(
                    "INSERT INTO bookings (booking_ref, idempotency_key, room_category_id, rate_plan_id, check_in_date, check_out_date, nights, rooms_count, adults, children, guest_name, guest_email, guest_phone, dpdp_consent_given, subtotal_before_discount_inr, discount_amount_inr, taxable_amount_inr, gst_percentage_applied, gst_amount_inr, total_payable_inr, status, expires_at) " +
                            "VALUES (?, ?, ?, 1, ?, ?, 2, 1, 2, 0, 'Race Guest', 'race@guest.com', '9876543210', 1, 17000.00, 0.00, 17000.00, 0.00, 0.00, 17000.00, 'PENDING_PAYMENT', ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                insertBooking.setString(1, bookingRef);
                insertBooking.setString(2, idempotencyKey);
                insertBooking.setLong(3, roomCategoryId);
                insertBooking.setDate(4, Date.valueOf(d1));
                insertBooking.setDate(5, Date.valueOf(d2.plusDays(1)));
                insertBooking.setTimestamp(6, Timestamp.from(Instant.now().minusSeconds(30)));
                insertBooking.executeUpdate();
                try (ResultSet rs = insertBooking.getGeneratedKeys()) {
                    assertTrue(rs.next());
                    bookingId = rs.getLong(1);
                }
            }

            try (PreparedStatement insertPayment = conn.prepareStatement(
                    "INSERT INTO payments (booking_id, one_time_payment_token, token_used, amount_inr, status) VALUES (?, ?, 0, 17000.00, 'INITIATED')")) {
                insertPayment.setLong(1, bookingId);
                insertPayment.setString(2, paymentToken);
                insertPayment.executeUpdate();
            }

            conn.commit();
        }

        // Now run webhook and expiry job concurrently
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        // Thread 1: webhook FAILURE
        pool.submit(() -> {
            try {
                startGun.await();
                bookingPaymentService.processMockWebhook(
                        bookingRef,
                        paymentToken,
                        new com.companya.resort.dto.PaymentWebhookRequest(paymentToken, "FAILURE")
                );
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: expiry job
        pool.submit(() -> {
            try {
                startGun.await();
                bookingExpiryService.releaseExpiredHolds();
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        startGun.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        pool.shutdown();

        // Verify that booked_count was decremented EXACTLY ONCE (from 1 to 0, not to -1)
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(
                     "SELECT stay_date, booked_count FROM daily_inventory_rates WHERE room_category_id = ? AND stay_date IN (?, ?)")) {
            checkStmt.setLong(1, roomCategoryId);
            checkStmt.setDate(2, Date.valueOf(d1));
            checkStmt.setDate(3, Date.valueOf(d2));
            try (ResultSet rs = checkStmt.executeQuery()) {
                while (rs.next()) {
                    int booked = rs.getInt("booked_count");
                    assertEquals(0, booked, "Stock must be released exactly once (booked_count = 0)");
                }
            }
        }
    }

    @Test
    void runScheduledExpiryJob_releasesExpiredPendingPaymentBookingAndStock() throws Exception {
        Long roomCategoryId = 1L;
        LocalDate d1 = LocalDate.of(2026, 12, 15);
        LocalDate d2 = LocalDate.of(2026, 12, 16);
        String bookingRef = "EXPIRE123456";
        String idempotencyKey = "expire-idem-" + System.currentTimeMillis();
        String paymentToken = "expire_token_" + System.currentTimeMillis();

        long bookingId;
        try (Connection conn = getConnection()) {
            try (PreparedStatement clean = conn.prepareStatement(
                    "DELETE FROM payments WHERE booking_id IN (SELECT id FROM bookings WHERE status = 'PENDING_PAYMENT' OR booking_ref = ? OR idempotency_key = ?)")) {
                clean.setString(1, bookingRef);
                clean.setString(2, idempotencyKey);
                clean.executeUpdate();
            }
            try (PreparedStatement clean = conn.prepareStatement(
                    "DELETE FROM bookings WHERE status = 'PENDING_PAYMENT' OR booking_ref = ? OR idempotency_key = ?")) {
                clean.setString(1, bookingRef);
                clean.setString(2, idempotencyKey);
                clean.executeUpdate();
            }
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM daily_inventory_rates WHERE room_category_id = ? AND stay_date IN (?, ?)")) {
                deleteStmt.setLong(1, roomCategoryId);
                deleteStmt.setDate(2, Date.valueOf(d1));
                deleteStmt.setDate(3, Date.valueOf(d2));
                deleteStmt.executeUpdate();
            }
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO daily_inventory_rates (room_category_id, stay_date, base_rate_inr, website_allotment_total, booked_count, version) VALUES (?, ?, ?, ?, ?, ?)")) {
                insertStmt.setLong(1, roomCategoryId);
                insertStmt.setDate(2, Date.valueOf(d1));
                insertStmt.setBigDecimal(3, new BigDecimal("8500.00"));
                insertStmt.setInt(4, 5);
                insertStmt.setInt(5, 1); // 1 unit held
                insertStmt.setLong(6, 0L);
                insertStmt.executeUpdate();

                insertStmt.setLong(1, roomCategoryId);
                insertStmt.setDate(2, Date.valueOf(d2));
                insertStmt.setBigDecimal(3, new BigDecimal("8500.00"));
                insertStmt.setInt(4, 5);
                insertStmt.setInt(5, 1); // 1 unit held
                insertStmt.setLong(6, 0L);
                insertStmt.executeUpdate();
            }

            conn.commit();
        }

        RoomCategory category = roomCategoryRepository.findById(roomCategoryId).orElseThrow();
        RatePlan ratePlan = ratePlanRepository.findById(1L).orElseThrow();

        Booking booking = new Booking();
        booking.setBookingRef(bookingRef);
        booking.setIdempotencyKey(idempotencyKey);
        booking.setRoomCategory(category);
        booking.setRatePlan(ratePlan);
        booking.setCheckInDate(d1);
        booking.setCheckOutDate(d2.plusDays(1));
        booking.setNights(2);
        booking.setRoomsCount(1);
        booking.setAdults(2);
        booking.setChildren(0);
        booking.setGuestName("Expire Guest");
        booking.setGuestEmail("expire@guest.com");
        booking.setGuestPhone("9876543210");
        booking.setDpdpConsentGiven(true);
        booking.setSubtotalBeforeDiscountInr(new BigDecimal("17000.00"));
        booking.setDiscountAmountInr(BigDecimal.ZERO);
        booking.setTaxableAmountInr(new BigDecimal("17000.00"));
        booking.setGstPercentageApplied(BigDecimal.ZERO);
        booking.setGstAmountInr(BigDecimal.ZERO);
        booking.setTotalPayableInr(new BigDecimal("17000.00"));
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setExpiresAt(LocalDateTime.now(clock).minusMinutes(2));
        booking = bookingRepository.save(booking);
        bookingId = booking.getId();

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setOneTimePaymentToken(paymentToken);
        payment.setTokenUsed(false);
        payment.setAmountInr(new BigDecimal("17000.00"));
        payment.setStatus(PaymentStatus.INITIATED);
        paymentRepository.save(payment);

        // Call the scheduled entry point (runScheduledExpiryJob)
        bookingExpiryService.runScheduledExpiryJob();

        // Verify that the booking became EXPIRED
        try (Connection conn = getConnection();
             PreparedStatement checkBooking = conn.prepareStatement("SELECT status FROM bookings WHERE id = ?")) {
            checkBooking.setLong(1, bookingId);
            try (ResultSet rs = checkBooking.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("EXPIRED", rs.getString("status"), "Booking status must be updated to EXPIRED");
            }
        }

        // Verify that booked_count was released (decremented from 1 back to 0)
        try (Connection conn = getConnection();
             PreparedStatement checkRates = conn.prepareStatement(
                     "SELECT stay_date, booked_count FROM daily_inventory_rates WHERE room_category_id = ? AND stay_date IN (?, ?)")) {
            checkRates.setLong(1, roomCategoryId);
            checkRates.setDate(2, Date.valueOf(d1));
            checkRates.setDate(3, Date.valueOf(d2));
            try (ResultSet rs = checkRates.executeQuery()) {
                while (rs.next()) {
                    int booked = rs.getInt("booked_count");
                    assertEquals(0, booked, "Stock must be released back to 0");
                }
            }
        }
    }

    @Test
    void when10ThreadsBookWithSingleUsePromoCode_exactly1SucceedsAnd9FailWithPromoExhausted() throws Exception {
        Long roomCatId = 1L;
        Long ratePlanId = 1L;
        LocalDate date1 = LocalDate.of(2026, 12, 10);
        LocalDate date2 = LocalDate.of(2026, 12, 11);
        String promoCodeStr = "CONCUR1";

        // 1. Reset inventory in test DB: 2 nights with website_allotment_total = 10, booked_count = 0
        try (Connection conn = getConnection()) {
            try (PreparedStatement deleteRates = conn.prepareStatement(
                    "DELETE FROM daily_inventory_rates WHERE room_category_id = ? AND stay_date IN (?, ?)")) {
                deleteRates.setLong(1, roomCatId);
                deleteRates.setDate(2, Date.valueOf(date1));
                deleteRates.setDate(3, Date.valueOf(date2));
                deleteRates.executeUpdate();
            }

            try (PreparedStatement insertRates = conn.prepareStatement(
                    "INSERT INTO daily_inventory_rates (room_category_id, stay_date, base_rate_inr, website_allotment_total, booked_count, version) VALUES (?, ?, ?, ?, ?, ?)")) {
                insertRates.setLong(1, roomCatId);
                insertRates.setDate(2, Date.valueOf(date1));
                insertRates.setBigDecimal(3, new BigDecimal("8500.00"));
                insertRates.setInt(4, 10);
                insertRates.setInt(5, 0);
                insertRates.setLong(6, 0L);
                insertRates.executeUpdate();

                insertRates.setDate(2, Date.valueOf(date2));
                insertRates.executeUpdate();
            }

            // 2. Insert or reset single-use promo code
            try (PreparedStatement deletePromo = conn.prepareStatement(
                    "DELETE FROM promo_codes WHERE code = ?")) {
                deletePromo.setString(1, promoCodeStr);
                deletePromo.executeUpdate();
            }

            try (PreparedStatement insertPromo = conn.prepareStatement(
                    "INSERT INTO promo_codes (code, discount_type, discount_value, valid_from, valid_to, min_nights, max_uses, current_uses, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                insertPromo.setString(1, promoCodeStr);
                insertPromo.setString(2, "PERCENTAGE");
                insertPromo.setBigDecimal(3, new BigDecimal("10.00"));
                insertPromo.setDate(4, Date.valueOf(LocalDate.of(2026, 1, 1)));
                insertPromo.setDate(5, Date.valueOf(LocalDate.of(2026, 12, 31)));
                insertPromo.setInt(6, 1);
                insertPromo.setInt(7, 1); // max_uses = 1
                insertPromo.setInt(8, 0); // current_uses = 0
                insertPromo.setBoolean(9, true);
                insertPromo.executeUpdate();
            }
            conn.commit();
        }

        // 3. Launch 10 concurrent booking requests
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger promoExhaustedCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CreateBookingRequest request = new CreateBookingRequest(
                            "idem-concur-promo-" + index,
                            roomCatId,
                            ratePlanId,
                            date1,
                            date2.plusDays(1),
                            1,
                            2,
                            0,
                            "Promo Concurrency Guest " + index,
                            "promo_concur_" + index + "@example.com",
                            "98765432" + String.format("%02d", index),
                            promoCodeStr,
                            true
                    );
                    bookingService.createBookingAndLockAllotment(request, "idem-concur-promo-" + index);
                    successCount.incrementAndGet();
                } catch (com.companya.resort.exception.InvalidPromoCodeException ex) {
                    if (ex.getMessage() != null && ex.getMessage().contains("maximum uses")) {
                        promoExhaustedCount.incrementAndGet();
                    } else {
                        otherErrorCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    otherErrorCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 10 concurrent threads must complete within timeout");
        assertEquals(1, successCount.get(), "Exactly 1 thread must successfully book with the single-use promo code");
        assertEquals(9, promoExhaustedCount.get(), "Exactly 9 threads must fail with promo maximum uses reached");
        assertEquals(0, otherErrorCount.get(), "No other errors must occur");

        // 4. Verify promo_codes current_uses in DB is exactly 1
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT current_uses, max_uses FROM promo_codes WHERE code = ?")) {
            ps.setString(1, promoCodeStr);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt("current_uses"), "Promo code current_uses in DB must be exactly 1");
                assertEquals(1, rs.getInt("max_uses"), "Promo code max_uses in DB must be 1");
            }
        }
    }
}
