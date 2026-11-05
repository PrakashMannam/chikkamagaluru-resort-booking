package com.companya.resort.util;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BudgetValidator {

    // Strict regex checking for explicit currency prefix or suffix/multiplier
    private static final Pattern STRICT_BUDGET_PATTERN = Pattern.compile(
            "(?:(?:\\b(?:Rs\\.?|INR)\\s*|₹\\s*)(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\s*(lakh|lakhs|cr|crore|k)?\\b|\\b(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\s*(lakh|lakhs|cr|crore|k|rupees|rupee|INR)\\b)",
            Pattern.CASE_INSENSITIVE
    );

    public static boolean containsExplicitBudget(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return false;
        }
        return STRICT_BUDGET_PATTERN.matcher(rawMessage).find();
    }

    public static BigDecimal extractBudgetIfExplicit(String rawMessage, BigDecimal aiExtractedBudget) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return null;
        }

        Matcher matcher = STRICT_BUDGET_PATTERN.matcher(rawMessage);
        if (!matcher.find()) {
            // No explicit currency marker or multiplier found in raw message -> ignore AI budget
            return null;
        }

        // Parse from raw message regex match
        String numStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
        String unit = matcher.group(2) != null ? matcher.group(2) : matcher.group(4);

        if (numStr == null) {
            return aiExtractedBudget;
        }

        try {
            double base = Double.parseDouble(numStr.replace(",", ""));
            double multiplier = 1.0;
            if (unit != null) {
                String u = unit.toLowerCase();
                if (u.startsWith("lakh")) {
                    multiplier = 100_000.0;
                } else if (u.startsWith("cr")) {
                    multiplier = 10_000_000.0;
                } else if (u.equals("k")) {
                    multiplier = 1_000.0;
                }
            }
            return BigDecimal.valueOf(base * multiplier).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (Exception e) {
            return aiExtractedBudget;
        }
    }
}
