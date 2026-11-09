package com.companya.resort.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Hardened security guard for integration tests.
 * Validates that connection URLs target an explicitly permitted, isolated,
 * disposable local test database before running migrations or test suites.
 *
 * Rejects unknown hosts, production/shared hosts, non-test database names,
 * and malformed URLs by default.
 * Enforces zero leakage of connection strings, hosts, database names, or credentials
 * in exception messages or logs.
 */
public final class DisposableDatabaseGuard {

    private static final String MYSQL_PREFIX = "jdbc:mysql://";
    private static final String MARIADB_PREFIX = "jdbc:mariadb://";

    // Strictly allowed loopback/isolated hosts
    private static final Set<String> ALLOWED_HOSTS;
    static {
        Set<String> hosts = new HashSet<>();
        hosts.add("localhost");
        hosts.add("127.0.0.1");
        hosts.add("::1");
        hosts.add("[::1]");
        ALLOWED_HOSTS = Collections.unmodifiableSet(hosts);
    }

    // Explicitly forbidden system databases
    private static final Set<String> FORBIDDEN_DATABASES;
    static {
        Set<String> dbs = new HashSet<>();
        dbs.add("mysql");
        dbs.add("information_schema");
        dbs.add("performance_schema");
        dbs.add("sys");
        FORBIDDEN_DATABASES = Collections.unmodifiableSet(dbs);
    }

    // Allowed disposable database name pattern: must be explicitly named as disposable or test
    // e.g. disposable_concurrency_repeat_test_db, resort_test, test_inventory, resort_test_db
    private static final Pattern ALLOWED_DB_NAME_PATTERN =
            Pattern.compile("^(disposable_[a-z0-9_]+|test_[a-z0-9_]+|[a-z0-9_]+_test|[a-z0-9_]+_test_db)$");

    private DisposableDatabaseGuard() {
        // utility class
    }

    /**
     * Validates that the provided JDBC URL strictly targets an approved, disposable local test database.
     * Throws IllegalStateException with generic, non-leaking messages if validation fails.
     *
     * @param rawJdbcUrl the JDBC connection string to evaluate
     * @throws IllegalStateException if the URL is missing, malformed, or targets a non-disposable host/db
     */
    public static void validateDisposableDatabase(String rawJdbcUrl) {
        if (rawJdbcUrl == null || rawJdbcUrl.trim().isEmpty()) {
            throw new IllegalStateException("Refusing to execute integration test: Database connection URL is missing or empty.");
        }

        String trimmed = rawJdbcUrl.trim();
        String withoutPrefix;
        if (trimmed.startsWith(MYSQL_PREFIX)) {
            withoutPrefix = trimmed.substring(MYSQL_PREFIX.length());
        } else if (trimmed.startsWith(MARIADB_PREFIX)) {
            withoutPrefix = trimmed.substring(MARIADB_PREFIX.length());
        } else {
            throw new IllegalStateException("Refusing to execute integration test: Target database configuration violates the disposable test policy (unsupported JDBC scheme).");
        }

        // Strip query parameters
        int queryIdx = withoutPrefix.indexOf('?');
        String hostAndPath = (queryIdx != -1) ? withoutPrefix.substring(0, queryIdx) : withoutPrefix;

        int slashIdx = hostAndPath.indexOf('/');
        if (slashIdx == -1) {
            throw new IllegalStateException("Refusing to execute integration test: Target database configuration violates the disposable test policy (missing database name).");
        }

        String rawAuthority = hostAndPath.substring(0, slashIdx).trim();
        String rawDbName = hostAndPath.substring(slashIdx + 1).trim();

        if (rawAuthority.isEmpty()) {
            throw new IllegalStateException("Refusing to execute integration test: Target database configuration violates the disposable test policy (missing host).");
        }
        if (rawDbName.isEmpty()) {
            throw new IllegalStateException("Refusing to execute integration test: Target database configuration violates the disposable test policy (missing database name).");
        }

        // If credentials are present (user:pass@host:port), strip user info to isolate host:port
        String hostPort = rawAuthority;
        int atIdx = rawAuthority.lastIndexOf('@');
        if (atIdx != -1) {
            hostPort = rawAuthority.substring(atIdx + 1).trim();
        }

        // Extract host from host:port
        String host;
        if (hostPort.startsWith("[")) {
            // IPv6 literal, e.g. [::1]:3306 or [::1]
            int closingBracket = hostPort.indexOf(']');
            if (closingBracket == -1) {
                throw new IllegalStateException("Refusing to execute integration test: Target database configuration is malformed.");
            }
            host = hostPort.substring(0, closingBracket + 1);
        } else {
            int colonIdx = hostPort.indexOf(':');
            host = (colonIdx != -1) ? hostPort.substring(0, colonIdx) : hostPort;
        }

        String normalizedHost = host.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_HOSTS.contains(normalizedHost)) {
            throw new IllegalStateException("Refusing to execute integration test: Target database host is not permitted by the disposable test policy.");
        }

        // Validate database name
        String normalizedDb = rawDbName.toLowerCase(Locale.ROOT);

        // Disallow path traversal, multiple segments, or dangerous punctuation
        if (normalizedDb.contains("/") || normalizedDb.contains("\\") || normalizedDb.contains(";") || normalizedDb.contains("#")) {
            throw new IllegalStateException("Refusing to execute integration test: Target database configuration is malformed.");
        }

        // Reject system databases
        if (FORBIDDEN_DATABASES.contains(normalizedDb)) {
            throw new IllegalStateException("Refusing to execute integration test: Target database name is not permitted by the disposable test policy.");
        }

        // Reject any database that explicitly hints at production/live/main/shared
        if (normalizedDb.contains("prod") || normalizedDb.contains("live") || normalizedDb.contains("main") || normalizedDb.contains("shared")) {
            throw new IllegalStateException("Refusing to execute integration test: Target database name is not permitted by the disposable test policy.");
        }

        // Must match explicit disposable/test naming pattern
        if (!ALLOWED_DB_NAME_PATTERN.matcher(normalizedDb).matches()) {
            throw new IllegalStateException("Refusing to execute integration test: Target database name is not permitted by the disposable test policy.");
        }
    }
}
