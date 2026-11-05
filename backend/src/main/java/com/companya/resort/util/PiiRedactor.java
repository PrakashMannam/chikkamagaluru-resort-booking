package com.companya.resort.util;

import java.util.regex.Pattern;

public class PiiRedactor {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            Pattern.CASE_INSENSITIVE
    );

    // Matches standard 10-digit Indian phone numbers with optional +91, 0 prefix, spaces or dashes
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:(?:\\+?91[\\s.-]?|0)?[6-9]\\d{4}[\\s.-]?\\d{5}|\\b[6-9]\\d{9}\\b)"
    );

    public static String redact(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }

        String result = EMAIL_PATTERN.matcher(message).replaceAll("[REDACTED_EMAIL]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[REDACTED_PHONE]");
        return result;
    }
}
