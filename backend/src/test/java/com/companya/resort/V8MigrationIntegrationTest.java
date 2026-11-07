package com.companya.resort;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class V8MigrationIntegrationTest {

    private static String testDbUrl;
    private static String testDbUser;
    private static String testDbPassword;

    @BeforeAll
    static void setUp() {
        testDbUrl = System.getenv("TEST_DB_URL");
        testDbUser = System.getenv("TEST_DB_USER");
        testDbPassword = System.getenv("TEST_DB_PASSWORD");

        // Allow fallback to standard local root credentials if TEST_DB_URL is provided or fallback
        if (testDbUrl == null || testDbUrl.isBlank()) {
            testDbUrl = "jdbc:mysql://localhost:3306/resort_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            testDbUser = "root";
            testDbPassword = "root";
        } else {
            if (testDbUser == null) testDbUser = "root";
            if (testDbPassword == null) testDbPassword = "";
        }

        // Test connectivity
        try (Connection conn = DriverManager.getConnection(testDbUrl, testDbUser, testDbPassword)) {
            // Connection successful
        } catch (SQLException e) {
            Assumptions.assumeTrue(false, "Skipping V8 migration test: database connection not available: " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(testDbUrl, testDbUser, testDbPassword);
    }

    @Test
    @DisplayName("V8 Migration is strictly idempotent and updates only pending ENQ-DEMO-CORP1 draft")
    void testV8MigrationTargetingAndIdempotence() throws Exception {
        String legacyText = "Dear Ms. Sen,\n\nThank you for considering our estate for your annual leadership offsite. Our conference pavilion features AV projection, high-speed WiFi, and panoramic plantation views, paired with guided cupping sessions.\n\nOur corporate specialist Meera Nair will contact you today with comprehensive proposal options.\n\nWarm regards,\nCorporate Events Team";
        String correctedText = "Dear Ms. Sen,\n\nThank you for considering our estate for your annual leadership offsite. Regarding meeting space, accommodation, and group activities for your team, our corporate specialist will confirm available facilities, room allotments, and event options with you directly.\n\nOur corporate specialist Meera Nair will contact you today with comprehensive proposal options.\n\nWarm regards,\nCorporate Events Team";

        // Read V8 SQL script
        String v8Sql;
        try (InputStream is = getClass().getResourceAsStream("/db/migration/V8__correct_corporate_demo_draft.sql")) {
            assertNotNull(is, "V8 migration SQL file must exist in classpath");
            v8Sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Remove SQL comments to extract executable UPDATE statement
        String executableUpdate = v8Sql.replaceAll("(?m)^--.*$", "").trim();

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                long pendingCorpEnqId;
                long sentCorpEnqId;
                long unrelatedEnqId;

                long pendingCorpDraftId;
                long sentCorpDraftId;
                long unrelatedDraftId;

                // 1. Insert test enquiries
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                            "INSERT INTO enquiries (enquiry_code, source_channel, sender_name, sender_contact, raw_message, category, status, received_at) " +
                            "VALUES ('ENQ-TEST-CORP-PENDING', 'EMAIL_SIM', 'Test Corp', 'corp@test.com', 'Raw Msg', 'CORPORATE_OFFSITE', 'OPEN', NOW())",
                            Statement.RETURN_GENERATED_KEYS
                    );
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        assertTrue(rs.next());
                        pendingCorpEnqId = rs.getLong(1);
                    }

                    stmt.executeUpdate(
                            "INSERT INTO enquiries (enquiry_code, source_channel, sender_name, sender_contact, raw_message, category, status, received_at) " +
                            "VALUES ('ENQ-TEST-CORP-SENT', 'EMAIL_SIM', 'Test Corp', 'corp@test.com', 'Raw Msg', 'CORPORATE_OFFSITE', 'OPEN', NOW())",
                            Statement.RETURN_GENERATED_KEYS
                    );
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        assertTrue(rs.next());
                        sentCorpEnqId = rs.getLong(1);
                    }

                    stmt.executeUpdate(
                            "INSERT INTO enquiries (enquiry_code, source_channel, sender_name, sender_contact, raw_message, category, status, received_at) " +
                            "VALUES ('ENQ-TEST-UNRELATED', 'EMAIL_SIM', 'Unrelated', 'unrelated@test.com', 'Raw Msg', 'CORPORATE_OFFSITE', 'OPEN', NOW())",
                            Statement.RETURN_GENERATED_KEYS
                    );
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        assertTrue(rs.next());
                        unrelatedEnqId = rs.getLong(1);
                    }
                }

                // 2. Insert drafts
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO enquiry_drafts (enquiry_id, draft_body, status, created_at) VALUES (?, ?, ?, NOW())",
                        Statement.RETURN_GENERATED_KEYS)) {

                    // Draft 1: Pending Corp with legacy text (TARGET)
                    ps.setLong(1, pendingCorpEnqId);
                    ps.setString(2, legacyText);
                    ps.setString(3, "DRAFT_PENDING_REVIEW");
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        assertTrue(rs.next());
                        pendingCorpDraftId = rs.getLong(1);
                    }

                    // Draft 2: Sent Corp with legacy text (MUST REMAIN UNTOUCHED - staff sent)
                    ps.setLong(1, sentCorpEnqId);
                    ps.setString(2, legacyText);
                    ps.setString(3, "SENT_MANUALLY");
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        assertTrue(rs.next());
                        sentCorpDraftId = rs.getLong(1);
                    }

                    // Draft 3: Unrelated enquiry with legacy text (MUST REMAIN UNTOUCHED - wrong enquiry code)
                    ps.setLong(1, unrelatedEnqId);
                    ps.setString(2, legacyText);
                    ps.setString(3, "DRAFT_PENDING_REVIEW");
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        assertTrue(rs.next());
                        unrelatedDraftId = rs.getLong(1);
                    }
                }

                // Temporary alias ENQ-TEST-CORP-PENDING and ENQ-TEST-CORP-SENT as ENQ-DEMO-CORP1 for testing the exact query
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("UPDATE enquiries SET enquiry_code = 'ENQ-DEMO-CORP1-TEMP-A' WHERE id = " + pendingCorpEnqId);
                    stmt.executeUpdate("UPDATE enquiries SET enquiry_code = 'ENQ-DEMO-CORP1-TEMP-B' WHERE id = " + sentCorpEnqId);
                }

                // Adjust test SQL to target our test pending and sent codes to test isolation cleanly
                String testSql = executableUpdate.replace("ENQ-DEMO-CORP1", "ENQ-DEMO-CORP1-TEMP-A");

                // Execute First Migration Run
                int firstRunUpdated;
                try (Statement stmt = conn.createStatement()) {
                    firstRunUpdated = stmt.executeUpdate(testSql);
                }

                assertEquals(1, firstRunUpdated, "First run of V8 must update exactly 1 pending demo draft");

                // Verify Draft 1 was updated to correctedText
                try (PreparedStatement ps = conn.prepareStatement("SELECT draft_body, status FROM enquiry_drafts WHERE id = ?")) {
                    ps.setLong(1, pendingCorpDraftId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(correctedText, rs.getString("draft_body"));
                        assertEquals("DRAFT_PENDING_REVIEW", rs.getString("status"));
                    }

                    // Verify Draft 2 (SENT_MANUALLY) was NOT modified
                    ps.setLong(1, sentCorpDraftId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(legacyText, rs.getString("draft_body"), "Sent draft must not be altered by V8");
                        assertEquals("SENT_MANUALLY", rs.getString("status"));
                    }

                    // Verify Draft 3 (Unrelated enquiry) was NOT modified
                    ps.setLong(1, unrelatedDraftId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(legacyText, rs.getString("draft_body"), "Unrelated draft must not be altered by V8");
                    }
                }

                // Execute Second Migration Run (Idempotency test)
                int secondRunUpdated;
                try (Statement stmt = conn.createStatement()) {
                    secondRunUpdated = stmt.executeUpdate(testSql);
                }

                assertEquals(0, secondRunUpdated, "Second run of V8 must update 0 rows (idempotence)");

            } finally {
                // Roll back test data cleanly so test database is not polluted
                conn.rollback();
            }
        }
    }
}
