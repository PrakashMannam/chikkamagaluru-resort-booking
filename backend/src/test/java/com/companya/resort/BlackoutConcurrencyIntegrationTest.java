package com.companya.resort;

import com.companya.resort.dto.AdminBlackoutDto;
import com.companya.resort.dto.CreateBlackoutRequest;
import com.companya.resort.entity.BlackoutDate;
import com.companya.resort.exception.BlackoutDateConflictException;
import com.companya.resort.repository.BlackoutDateRepository;
import com.companya.resort.repository.RoomCategoryRepository;
import com.companya.resort.service.AdminBlackoutPromoService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class BlackoutConcurrencyIntegrationTest {

    private static String testDbUrl;
    private static String testDbUser;
    private static String testDbPassword;
    private static ConfigurableApplicationContext applicationContext;

    private static AdminBlackoutPromoService adminBlackoutPromoService;
    private static BlackoutDateRepository blackoutDateRepository;
    private static RoomCategoryRepository roomCategoryRepository;

    @BeforeAll
    static void setUp() {
        testDbUrl = System.getenv("TEST_DB_URL");
        testDbUser = System.getenv("TEST_DB_USER");
        testDbPassword = System.getenv("TEST_DB_PASSWORD");

        Assumptions.assumeTrue(
                testDbUrl != null && !testDbUrl.trim().isEmpty(),
                "Skipping BlackoutConcurrencyIntegrationTest: TEST_DB_URL environment variable is not set."
        );

        if (testDbUser == null) {
            testDbUser = "root";
        }
        if (testDbPassword == null) {
            testDbPassword = "";
        }

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(testDbUrl, testDbUser, testDbPassword)
                    .cleanDisabled(false)
                    .load();
            flyway.migrate();

            applicationContext = new SpringApplicationBuilder(ResortApplication.class)
                    .profiles("demo")
                    .web(org.springframework.boot.WebApplicationType.NONE)
                    .properties(
                            "DB_URL=" + testDbUrl,
                            "DB_USER=" + testDbUser,
                            "DB_PASSWORD=" + testDbPassword,
                            "spring.datasource.url=" + testDbUrl,
                            "spring.datasource.username=" + testDbUser,
                            "spring.datasource.password=" + testDbPassword,
                            "spring.jpa.hibernate.ddl-auto=validate",
                            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
                            "spring.flyway.enabled=false",
                            "JWT_SECRET=supersecretkeyforresortbookingapp2026demo123456",
                            "CORS_ALLOWED_ORIGIN=http://localhost:5173"
                    )
                    .run();

            adminBlackoutPromoService = applicationContext.getBean(AdminBlackoutPromoService.class);
            blackoutDateRepository = applicationContext.getBean(BlackoutDateRepository.class);
            roomCategoryRepository = applicationContext.getBean(RoomCategoryRepository.class);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Skipping BlackoutConcurrencyIntegrationTest: could not connect to MySQL resort_test (" + e.getMessage() + ")");
        }
    }

    @AfterAll
    static void tearDown() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    @Test
    @DisplayName("Multiple simultaneous resort-wide blackout creation requests in MySQL: exactly 1 persists, all others receive 409 conflict")
    void testSimultaneousResortWideBlackoutConcurrency() throws Exception {
        LocalDate testDate = LocalDate.of(2027, 6, 15);
        cleanupDate(testDate);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<AdminBlackoutDto>> futures = new ArrayList<>();

        for (int i = 1; i <= threadCount; i++) {
            final String reason = "Concurrent Resort Buyout Thread " + i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await(5, TimeUnit.SECONDS);
                return adminBlackoutPromoService.createBlackout(new CreateBlackoutRequest(null, null, testDate, reason));
            }));
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "Threads failed to synchronize in time");
        startLatch.countDown();

        int successCount = 0;
        int conflictCount = 0;
        AdminBlackoutDto winningDto = null;

        for (Future<AdminBlackoutDto> future : futures) {
            try {
                AdminBlackoutDto dto = future.get(10, TimeUnit.SECONDS);
                successCount++;
                winningDto = dto;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof BlackoutDateConflictException) {
                    conflictCount++;
                    assertTrue(cause.getMessage().contains("A resort-wide blackout date already exists on " + testDate),
                            "Conflict message must explain existing resort-wide blackout: " + cause.getMessage());
                } else {
                    fail("Unexpected exception from concurrent blackout creation: " + cause);
                }
            }
        }

        executor.shutdown();

        assertEquals(1, successCount, "Exactly one resort-wide blackout creation must succeed");
        assertEquals(threadCount - 1, conflictCount, "All other concurrent requests must receive BlackoutDateConflictException");
        assertNotNull(winningDto, "Winning DTO must not be null");

        List<BlackoutDate> persisted = blackoutDateRepository.findBlackoutsBetweenInclusive(testDate, testDate);
        assertEquals(1, persisted.size(), "Database must contain exactly 1 blackout record for the contested date");
        assertNull(persisted.get(0).getRoomCategory(), "Persisted record must be resort-wide (roomCategory IS NULL)");
        assertEquals(testDate, persisted.get(0).getBlackoutDate());

        cleanupDate(testDate);
    }

    @Test
    @DisplayName("Sequential duplicate resort-wide blackout creation request is rejected with 409 BlackoutDateConflictException")
    void testSequentialDuplicateResortWideBlackout() {
        LocalDate testDate = LocalDate.of(2027, 7, 20);
        cleanupDate(testDate);

        AdminBlackoutDto first = adminBlackoutPromoService.createBlackout(
                new CreateBlackoutRequest(null, null, testDate, "First Resort Buyout")
        );
        assertNotNull(first);
        assertEquals(testDate, first.blackoutDate());

        BlackoutDateConflictException ex = assertThrows(BlackoutDateConflictException.class, () ->
                adminBlackoutPromoService.createBlackout(new CreateBlackoutRequest(null, null, testDate, "Duplicate Resort Buyout"))
        );
        assertTrue(ex.getMessage().contains("A resort-wide blackout date already exists on " + testDate));

        List<BlackoutDate> persisted = blackoutDateRepository.findBlackoutsBetweenInclusive(testDate, testDate);
        assertEquals(1, persisted.size(), "Database must contain exactly 1 blackout record");

        cleanupDate(testDate);
    }

    @Test
    @DisplayName("Multiple simultaneous category blackout creation requests for the same category: exactly 1 persists, all others receive 409 conflict")
    void testSimultaneousCategoryBlackoutConcurrency() throws Exception {
        LocalDate testDate = LocalDate.of(2027, 9, 5);
        cleanupDate(testDate);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<AdminBlackoutDto>> futures = new ArrayList<>();

        for (int i = 1; i <= threadCount; i++) {
            final String reason = "Concurrent Room Maintenance " + i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await(5, TimeUnit.SECONDS);
                return adminBlackoutPromoService.createBlackout(new CreateBlackoutRequest("ROOM", null, testDate, reason));
            }));
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "Threads failed to synchronize in time");
        startLatch.countDown();

        int successCount = 0;
        int conflictCount = 0;
        AdminBlackoutDto winningDto = null;

        for (Future<AdminBlackoutDto> future : futures) {
            try {
                AdminBlackoutDto dto = future.get(10, TimeUnit.SECONDS);
                successCount++;
                winningDto = dto;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof BlackoutDateConflictException) {
                    conflictCount++;
                    assertTrue(cause.getMessage().contains("already exists on " + testDate),
                            "Conflict message must explain existing blackout: " + cause.getMessage());
                } else {
                    fail("Unexpected exception from concurrent blackout creation: " + cause);
                }
            }
        }

        executor.shutdown();

        assertEquals(1, successCount, "Exactly one category blackout creation must succeed");
        assertEquals(threadCount - 1, conflictCount, "All other concurrent requests must receive BlackoutDateConflictException");
        assertNotNull(winningDto, "Winning DTO must not be null");

        List<BlackoutDate> persisted = blackoutDateRepository.findBlackoutsBetweenInclusive(testDate, testDate);
        assertEquals(1, persisted.size(), "Database must contain exactly 1 blackout record for the date");
        assertNotNull(persisted.get(0).getRoomCategory(), "Persisted record must have room category");
        assertEquals("ROOM", persisted.get(0).getRoomCategory().getCode());

        cleanupDate(testDate);
    }

    @Test
    @DisplayName("Category-specific blackouts on distinct categories for the same date succeed without conflict")
    void testDistinctCategoriesOnSameDateSucceed() {
        LocalDate testDate = LocalDate.of(2027, 8, 10);
        cleanupDate(testDate);

        AdminBlackoutDto roomBlackout = adminBlackoutPromoService.createBlackout(
                new CreateBlackoutRequest("ROOM", null, testDate, "Room deep clean")
        );
        AdminBlackoutDto villaBlackout = adminBlackoutPromoService.createBlackout(
                new CreateBlackoutRequest("VILLA", null, testDate, "Villa pool service")
        );

        assertNotNull(roomBlackout);
        assertNotNull(villaBlackout);
        assertEquals("ROOM", roomBlackout.roomCategoryCode());
        assertEquals("VILLA", villaBlackout.roomCategoryCode());

        List<BlackoutDate> persisted = blackoutDateRepository.findBlackoutsBetweenInclusive(testDate, testDate);
        assertEquals(2, persisted.size(), "Both distinct category blackouts must coexist on the same date");

        cleanupDate(testDate);
    }

    private void cleanupDate(LocalDate date) {
        List<BlackoutDate> existing = blackoutDateRepository.findBlackoutsBetweenInclusive(date, date);
        if (!existing.isEmpty()) {
            blackoutDateRepository.deleteAll(existing);
            blackoutDateRepository.flush();
        }
    }
}
