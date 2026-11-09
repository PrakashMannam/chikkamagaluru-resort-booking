package com.companya.resort.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DisposableDatabaseGuardTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://localhost:3306/disposable_concurrency_repeat_test_db",
            "jdbc:mysql://localhost:3306/disposable_concurrency_repeat_test_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            "jdbc:mysql://127.0.0.1:3306/resort_test",
            "jdbc:mysql://127.0.0.1:3306/resort_test_db",
            "jdbc:mysql://127.0.0.1:3306/test_inventory_hold",
            "jdbc:mysql://[::1]:3306/disposable_unit_test",
            "jdbc:mysql://localhost/test_bookings",
            "jdbc:mariadb://localhost:3306/disposable_test"
    })
    @DisplayName("Allowed disposable configuration passes validation cleanly")
    void testAllowedDisposableUrls(String validUrl) {
        assertDoesNotThrow(() -> DisposableDatabaseGuard.validateDisposableDatabase(validUrl));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://production.railway.internal:3306/disposable_test_db",
            "jdbc:mysql://production.railway.internal:3306/resort_test",
            "jdbc:mysql://db.prod.resort.com:3306/resort_test",
            "jdbc:mysql://shared-staging.internal:3306/test_db",
            "jdbc:mysql://aws-rds-east.amazonaws.com:3306/disposable_test",
            "jdbc:mysql://192.168.1.100:3306/test_inventory",
            "jdbc:mysql://10.0.0.5:3306/disposable_concurrency_test",
            "jdbc:mysql://0.0.0.0:3306/disposable_test",
            "jdbc:mysql://remote-test-host.org:3306/resort_test"
    })
    @DisplayName("Production and shared hosts are rejected even if database name contains test or disposable")
    void testRejectNonLocalHostsEvenWithTestDatabase(String nonLocalUrl) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                DisposableDatabaseGuard.validateDisposableDatabase(nonLocalUrl)
        );
        assertEquals("Refusing to execute integration test: Target database host is not permitted by the disposable test policy.", ex.getMessage());

        // Zero-leakage verification: ensure forbidden host or URL substring does not appear in exception
        assertFalse(ex.getMessage().contains("production"));
        assertFalse(ex.getMessage().contains("railway"));
        assertFalse(ex.getMessage().contains("amazonaws"));
        assertFalse(ex.getMessage().contains("shared-staging"));
        assertFalse(ex.getMessage().contains("192.168.1.100"));
        assertFalse(ex.getMessage().contains("remote-test-host"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://localhost:3306/resort",
            "jdbc:mysql://localhost:3306/resort_production",
            "jdbc:mysql://localhost:3306/resort_prod_test",
            "jdbc:mysql://localhost:3306/resort_live",
            "jdbc:mysql://localhost:3306/main_booking_db",
            "jdbc:mysql://localhost:3306/shared_resort",
            "jdbc:mysql://localhost:3306/mysql",
            "jdbc:mysql://localhost:3306/information_schema",
            "jdbc:mysql://localhost:3306/performance_schema",
            "jdbc:mysql://localhost:3306/sys",
            "jdbc:mysql://localhost:3306/custom_unmarked_database"
    })
    @DisplayName("Disallowed, ambiguous, or system database names are rejected even on localhost")
    void testRejectDisallowedDatabaseNames(String disallowedDbUrl) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                DisposableDatabaseGuard.validateDisposableDatabase(disallowedDbUrl)
        );
        assertEquals("Refusing to execute integration test: Target database name is not permitted by the disposable test policy.", ex.getMessage());

        // Zero-leakage verification
        assertFalse(ex.getMessage().contains("resort"));
        assertFalse(ex.getMessage().contains("production"));
        assertFalse(ex.getMessage().contains("mysql"));
        assertFalse(ex.getMessage().contains("information_schema"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://localhost:3306/",
            "jdbc:mysql://localhost:3306",
            "jdbc:mysql:///resort_test",
            "jdbc:postgresql://localhost:5432/resort_test",
            "http://localhost:3306/resort_test",
            "not_a_valid_url",
            "jdbc:mysql://localhost:3306/test/nested/path",
            "jdbc:mysql://localhost:3306/test_db;drop table users"
    })
    @DisplayName("Malformed, incomplete, or wrong-scheme URLs are rejected")
    void testRejectMalformedUrls(String malformedUrl) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                DisposableDatabaseGuard.validateDisposableDatabase(malformedUrl)
        );
        assertTrue(ex.getMessage().startsWith("Refusing to execute integration test:"));
        assertFalse(ex.getMessage().contains("postgresql"));
        assertFalse(ex.getMessage().contains("drop table"));
    }

    @Test
    @DisplayName("Null and blank connection strings are rejected")
    void testNullAndBlankRejected() {
        assertThrows(IllegalStateException.class, () -> DisposableDatabaseGuard.validateDisposableDatabase(null));
        assertThrows(IllegalStateException.class, () -> DisposableDatabaseGuard.validateDisposableDatabase(""));
        assertThrows(IllegalStateException.class, () -> DisposableDatabaseGuard.validateDisposableDatabase("   "));
    }

    @Test
    @DisplayName("Embedded credentials in URL do not leak into exception messages or logs")
    void testZeroCredentialLeakage() {
        String sensitiveUrl = "jdbc:mysql://super_secret_admin_user:super_secret_admin_password_98765@db.production.railway.internal:3306/disposable_test";

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                DisposableDatabaseGuard.validateDisposableDatabase(sensitiveUrl)
        );

        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().contains("super_secret_admin_user"), "Username must never leak in exception message");
        assertFalse(ex.getMessage().contains("super_secret_admin_password_98765"), "Password must never leak in exception message");
        assertFalse(ex.getMessage().contains("db.production.railway.internal"), "Host must never leak in exception message");
        assertFalse(ex.getMessage().contains("jdbc:mysql"), "URL scheme must never leak in exception message");
    }
}
