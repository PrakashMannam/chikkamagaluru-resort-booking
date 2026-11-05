package com.companya.resort.util;

import java.util.regex.Pattern;

public class DraftPriceSanitizer {

    public static final String QUOTE_BLOCK_PLACEHOLDER = "{{RATE_TABLE_QUOTE_BLOCK}}";

    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "(?i)(?:(?:₹|Rs\\.?|INR|rupees?)\\s*\\d[\\d,]*(?:\\.\\d+)?(?:\\s*(?:lakh|lakhs|cr|crore|k|only|/-))?|\\b\\d[\\d,]*(?:\\.\\d+)?\\s*(?:₹|Rs\\.?|INR|rupees?|lakh|lakhs|cr|crore)\\b|\\b(?:one|two|three|four|five|ten|hundred|thousand)\\s+rupees?\\b|\\b(?:rupees?|rs\\.?|inr)\\b)"
    );

    public static String sanitize(String rawDraft) {
        if (rawDraft == null || rawDraft.isBlank()) {
            return QUOTE_BLOCK_PLACEHOLDER;
        }

        // Replace any invented price patterns with placeholder
        String sanitized = PRICE_PATTERN.matcher(rawDraft).replaceAll(QUOTE_BLOCK_PLACEHOLDER);

        // Deduplicate consecutive placeholders if multiple matched
        while (sanitized.contains(QUOTE_BLOCK_PLACEHOLDER + " " + QUOTE_BLOCK_PLACEHOLDER) ||
               sanitized.contains(QUOTE_BLOCK_PLACEHOLDER + QUOTE_BLOCK_PLACEHOLDER)) {
            sanitized = sanitized.replace(QUOTE_BLOCK_PLACEHOLDER + " " + QUOTE_BLOCK_PLACEHOLDER, QUOTE_BLOCK_PLACEHOLDER);
            sanitized = sanitized.replace(QUOTE_BLOCK_PLACEHOLDER + QUOTE_BLOCK_PLACEHOLDER, QUOTE_BLOCK_PLACEHOLDER);
        }

        return sanitized;
    }
}
