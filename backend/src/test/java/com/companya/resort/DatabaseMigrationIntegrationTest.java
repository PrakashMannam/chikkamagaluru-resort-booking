package com.companya.resort;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationIntegrationTest {

    private static String testDbUrl;
    private static String testDbUser;
    private static String testDbPassword;

    @BeforeAll
    static void setUp() {
        testDbUrl = System.getenv("TEST_DB_URL");
        testDbUser = System.getenv("TEST_DB_USER");
        testDbPassword = System.getenv("TEST_DB_PASSWORD");

        Assumptions.assumeTrue(
            testDbUrl != null && !testDbUrl.trim().isEmpty(),
            "Skipping migration test: TEST_DB_URL environment variable is not set."
        );

        if (testDbUser == null) {
            testDbUser = "root";
        }
        if (testDbPassword == null) {
            testDbPassword = "";
        }

        // Run Flyway migrations on the separate test database
        Flyway flyway = Flyway.configure()
            .dataSource(testDbUrl, testDbUser, testDbPassword)
            .load();
        flyway.migrate();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(testDbUrl, testDbUser, testDbPassword);
    }

    @Test
    void allTwelveTablesExist() throws SQLException {
        Set<String> tables = new HashSet<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString(1).toLowerCase());
                }
            }
        }

        List<String> expectedTables = List.of(
            "users",
            "room_categories",
            "physical_units",
            "rate_plans",
            "daily_inventory_rates",
            "blackout_dates",
            "promo_codes",
            "gst_tax_slabs",
            "bookings",
            "payments",
            "enquiries",
            "enquiry_drafts"
        );

        for (String expected : expectedTables) {
            assertTrue(tables.contains(expected), "Expected table '" + expected + "' to exist in test database");
        }
    }

    @Test
    void staticReferenceDataIsSeededCorrectly() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Assert 2 categories
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM room_categories")) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1), "Expected 2 room categories");
            }

            // Assert 76 physical units
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM physical_units")) {
                assertTrue(rs.next());
                assertEquals(76, rs.getInt(1), "Expected 76 physical units");
            }

            // Assert 6 rate plans (3 per category)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rate_plans")) {
                assertTrue(rs.next());
                assertEquals(6, rs.getInt(1), "Expected 6 rate plans (3 per category)");
            }
        }
    }

    @Test
    void dailyInventoryRatesRejectsBookedCountExceedingWebsiteAllotment() throws SQLException {
        try (Connection conn = getConnection()) {
            // Attempt to insert row where booked_count (15) > website_allotment_total (10)
            assertThrows(SQLException.class, () -> {
                try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO daily_inventory_rates (room_category_id, stay_date, base_rate_inr, website_allotment_total, booked_count, version) " +
                    "VALUES (1, '2099-12-31', 8500.00, 10, 15, 0)")) {
                    stmt.executeUpdate();
                }
            }, "Expected CHECK constraint chk_daily_inventory_rates_booked_count to reject booked_count > website_allotment_total");
        }
    }
}
