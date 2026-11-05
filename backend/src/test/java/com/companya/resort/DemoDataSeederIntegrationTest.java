package com.companya.resort;

import com.companya.resort.entity.DailyInventoryRate;
import com.companya.resort.entity.RoomCategory;
import com.companya.resort.entity.User;
import com.companya.resort.repository.DailyInventoryRateRepository;
import com.companya.resort.repository.RoomCategoryRepository;
import com.companya.resort.repository.UserRepository;
import com.companya.resort.seeder.DemoDataSeeder;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DemoDataSeederIntegrationTest {

    private static String testDbUrl;
    private static String testDbUser;
    private static String testDbPassword;
    private static ConfigurableApplicationContext applicationContext;
    private static DemoDataSeeder demoDataSeeder;
    private static UserRepository userRepository;
    private static RoomCategoryRepository roomCategoryRepository;
    private static DailyInventoryRateRepository dailyInventoryRateRepository;

    @BeforeAll
    static void setUp() {
        testDbUrl = System.getenv("TEST_DB_URL");
        testDbUser = System.getenv("TEST_DB_USER");
        testDbPassword = System.getenv("TEST_DB_PASSWORD");

        if (testDbUrl == null || testDbUrl.isBlank()) {
            testDbUrl = "jdbc:mysql://localhost:3306/resort_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }
        if (testDbUser == null) {
            testDbUser = "root";
        }
        if (testDbPassword == null) {
            testDbPassword = "root";
        }

        try {
            // Apply Flyway V1-V4 migrations to resort_test
            Flyway flyway = Flyway.configure()
                    .dataSource(testDbUrl, testDbUser, testDbPassword)
                    .cleanDisabled(false)
                    .load();
            flyway.clean();
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
                            "spring.jpa.hibernate.ddl-auto=none",
                            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
                            "spring.flyway.enabled=false",
                            "JWT_SECRET=supersecretkeyforresortbookingapp2026demo123456",
                            "CORS_ALLOWED_ORIGIN=http://localhost:5173"
                    )
                    .run();

            demoDataSeeder = applicationContext.getBean(DemoDataSeeder.class);
            userRepository = applicationContext.getBean(UserRepository.class);
            roomCategoryRepository = applicationContext.getBean(RoomCategoryRepository.class);
            dailyInventoryRateRepository = applicationContext.getBean(DailyInventoryRateRepository.class);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Skipping DemoDataSeederIntegrationTest: could not connect to MySQL resort_test (" + e.getMessage() + ")");
        }
    }

    @AfterAll
    static void tearDown() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    @Test
    void testDemoProfileSeedsDataCorrectlyAndIsIdempotent() {
        assertNotNull(demoDataSeeder, "DemoDataSeeder bean must be present in demo profile");

        // Verify users seeded
        List<User> users = userRepository.findAll();
        assertEquals(4, users.size(), "Demo profile must seed exactly 4 staff users");

        // Verify inventory seeded for 181 days (2026-11-01 to 2027-04-30) * 2 categories = 362 records
        List<DailyInventoryRate> allRates = dailyInventoryRateRepository.findAll();
        assertEquals(362, allRates.size(), "Demo profile must seed 362 inventory records (181 days * 2 categories)");

        RoomCategory roomCat = roomCategoryRepository.findByCode("ROOM").orElseThrow();
        RoomCategory villaCat = roomCategoryRepository.findByCode("VILLA").orElseThrow();

        // Check Sunday Nov 1, 2026: ROOM base 8500, allotment 15; VILLA base 15000, allotment 4
        DailyInventoryRate roomNov1 = dailyInventoryRateRepository.findByRoomCategoryIdAndStayDate(roomCat.getId(), LocalDate.of(2026, 11, 1)).orElseThrow();
        assertEquals(0, new BigDecimal("8500.00").compareTo(roomNov1.getBaseRateInr()));
        assertEquals(15, roomNov1.getWebsiteAllotmentTotal());
        assertEquals(0, roomNov1.getBookedCount());

        DailyInventoryRate villaNov1 = dailyInventoryRateRepository.findByRoomCategoryIdAndStayDate(villaCat.getId(), LocalDate.of(2026, 11, 1)).orElseThrow();
        assertEquals(0, new BigDecimal("15000.00").compareTo(villaNov1.getBaseRateInr()));
        assertEquals(4, villaNov1.getWebsiteAllotmentTotal());

        // Check Friday Nov 6, 2026: ROOM base 10500; VILLA base 18000
        DailyInventoryRate roomNov6 = dailyInventoryRateRepository.findByRoomCategoryIdAndStayDate(roomCat.getId(), LocalDate.of(2026, 11, 6)).orElseThrow();
        assertEquals(0, new BigDecimal("10500.00").compareTo(roomNov6.getBaseRateInr()));

        DailyInventoryRate villaNov6 = dailyInventoryRateRepository.findByRoomCategoryIdAndStayDate(villaCat.getId(), LocalDate.of(2026, 11, 6)).orElseThrow();
        assertEquals(0, new BigDecimal("18000.00").compareTo(villaNov6.getBaseRateInr()));

        // Check Nov 14 and Nov 15: VILLA allotment override = 2
        DailyInventoryRate villaNov14 = dailyInventoryRateRepository.findByRoomCategoryIdAndStayDate(villaCat.getId(), LocalDate.of(2026, 11, 14)).orElseThrow();
        assertEquals(2, villaNov14.getWebsiteAllotmentTotal());

        DailyInventoryRate villaNov15 = dailyInventoryRateRepository.findByRoomCategoryIdAndStayDate(villaCat.getId(), LocalDate.of(2026, 11, 15)).orElseThrow();
        assertEquals(2, villaNov15.getWebsiteAllotmentTotal());

        // IDEMPOTENCY TEST: Run runner again
        demoDataSeeder.run();

        // Counts must remain identical
        assertEquals(4, userRepository.findAll().size(), "Rerunning seeder must not create duplicate users");
        assertEquals(362, dailyInventoryRateRepository.findAll().size(), "Rerunning seeder must not create duplicate rates");
    }
}
