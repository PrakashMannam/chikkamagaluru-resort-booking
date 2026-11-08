package com.companya.resort.util;

import java.util.regex.Pattern;

public class PiiRedactor {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            Pattern.CASE_INSENSITIVE
    );

    // Matches standard Indian mobile/landline numbers and international formats with optional + prefix, spaces or dashes
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:(?:\\+?91[\\s.-]?|0)?[6-9]\\d{4}[\\s.-]?\\d{5}|\\b[6-9]\\d{9}\\b|(?:\\+?91[\\s.-]?|0)\\d{2,4}[\\s.-]?\\d{6,8}|\\+\\d{1,3}[\\s.-]?\\(?\\d{1,4}\\)?[\\s.-]?\\d{3,4}[\\s.-]?\\d{3,4})"
    );
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("(?<!\\d)(?:\\d{4}[\\s-]?){2}\\d{4}(?!\\d)");
    private static final Pattern PAN_PATTERN = Pattern.compile("\\b[A-Z]{5}\\d{4}[A-Z]\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("\\b[A-Z]\\d{7}\\b", Pattern.CASE_INSENSITIVE);

    public static String redact(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }

        String result = EMAIL_PATTERN.matcher(message).replaceAll("[REDACTED_EMAIL]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[REDACTED_PHONE]");
        result = AADHAAR_PATTERN.matcher(result).replaceAll("[REDACTED_ID]");
        result = PAN_PATTERN.matcher(result).replaceAll("[REDACTED_ID]");
        result = PASSPORT_PATTERN.matcher(result).replaceAll("[REDACTED_ID]");
        return result;
    }
}
