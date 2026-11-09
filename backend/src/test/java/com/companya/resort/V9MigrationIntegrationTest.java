package com.companya.resort;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class V9MigrationIntegrationTest {

    private static String testDbUrl;
    private static String testDbUser;
    private static String testDbPassword;

    @BeforeAll
    static void setUp() {
        testDbUrl = System.getenv("TEST_DB_URL");
        testDbUser = System.getenv("TEST_DB_USER");
        testDbPassword = System.getenv("TEST_DB_PASSWORD");

        if (testDbUrl == null || testDbUrl.isBlank()) {
            testDbUrl = "jdbc:mysql://localhost:3306/resort_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            testDbUser = "root";
            testDbPassword = "root";
        } else {
            if (testDbUser == null) testDbUser = "root";
            if (testDbPassword == null) testDbPassword = "";
        }

        try (Connection conn = DriverManager.getConnection(testDbUrl, testDbUser, testDbPassword)) {
            // Connected successfully
        } catch (SQLException e) {
            Assumptions.assumeTrue(false, "Skipping V9 migration test: database connection not available: " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(testDbUrl, testDbUser, testDbPassword);
    }

    @Test
    @DisplayName("V9 migration verifies is_active column exists and shared demo accounts are disabled")
    void testV9SchemaAndDeactivatedAccounts() throws Exception {
        try (Connection conn = getConnection()) {
            // 1. Verify is_active column exists on users table
            DatabaseMetaData metaData = conn.getMetaData();
            boolean hasIsActive = false;
            try (ResultSet rs = metaData.getColumns(null, null, "users", "is_active")) {
                if (rs.next()) {
                    hasIsActive = true;
                }
            }
            assertTrue(hasIsActive, "users table must contain is_active column after V9 migration");

            // 2. Verify all 4 shared demo accounts are marked is_active = FALSE if present
            String query = "SELECT email, is_active FROM users WHERE email IN ('gm.demo@companya-estate.test', 'rooms.demo@companya-estate.test', 'weddings.demo@companya-estate.test', 'offsites.demo@companya-estate.test')";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    String email = rs.getString("email");
                    boolean isActive = rs.getBoolean("is_active");
                    assertFalse(isActive, "Public shared demo account " + email + " must have is_active = FALSE");
                }
            }
        }
    }
}
