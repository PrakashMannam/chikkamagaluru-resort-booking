package com.companya.resort.util;

import java.util.regex.Pattern;

public class DraftPriceSanitizer {

    public static final String QUOTE_BLOCK_PLACEHOLDER = "{{RATE_TABLE_QUOTE_BLOCK}}";

    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "(?i)(?:(?:₹|Rs\\.?|INR|rupees?)\\s*\\d[\\d,]*(?:\\.\\d+)?(?:\\s*(?:lakh|lakhs|cr|crore|k|only|/-))?|\\b\\d[\\d,]*(?:\\.\\d+)?\\s*(?:₹|Rs\\.?|INR|rupees?|lakh|lakhs|cr|crore)\\b|\\b(?:one|two|three|four|five|ten|hundred|thousand)\\s+rupees?\\b|\\b(?:rupees?|rs\\.?|inr)\\b)"
    );
    private static final Pattern BARE_RATE_CLAIM_PATTERN = Pattern.compile(
            "(?i)\\b(?:(?:nightly|room|daily)\\s+)?(?:rate|price|cost|tariff|total)(?:\\s+(?:amount|payable))?\\s*(?:(?:is|of|at|would\\s+be|comes\\s+to)\\s*)?\\d[\\d,]*(?:\\.\\d+)?(?:\\s*(?:per\\s+night|/\\s*night))?"
    );

    // Matches unsupported room hold claims
    private static final Pattern HOLD_PATTERN = Pattern.compile(
            "(?i)\\b(?:we\\s+have\\s+held|we\\s+held|have\\s+held|room(?:\\s+has\\s+been|\\s+is)\\s+held|held\\s+a\\s+(?:private|pool|villa|room|luxury)|allotment\\s+has\\s+been\\s+held)\\b[^.!?\\n]*[.!?]?"
    );

    // Matches unverified meal and activity inclusion assertions
    private static final Pattern MEAL_INCLUSION_PATTERN = Pattern.compile(
            "(?i)\\b(?:includes?\\s+gourmet\\s+breakfast|complimentary\\s+plantation\\s+walk|tariff\\s+includes\\s+gourmet|complimentary\\s+breakfast|includes?\\s+all\\s+meals|(?:breakfast|dinner|lunch|meals?)\\b[^.!?\\n]*\\b(?:included|complimentary|free)|(?:includes?|offers?|provides?|serves?)\\b[^.!?\\n]*\\b(?:breakfast|dinner|lunch|meals?))\\b[^.!?\\n]*[.!?]?"
    );

    // Matches unsupported facility, equipment, and unverified group activity assertions
    private static final Pattern UNSUPPORTED_FACILITY_ACTIVITY_PATTERN = Pattern.compile(
            "(?i)\\b(?:features?\\s+(?:AV\\s+projection|projector|high-speed\\s+wi-?fi|panoramic\\s+plantation\\s+views?)|paired\\s+with\\s+guided\\s+cupping|guided\\s+cupping\\s+sessions?|complimentary\\s+(?:airport\\s+transit|spa|shuttle)|free\\s+(?:airport\\s+transit|spa|shuttle))\\b[^.!?\\n]*[.!?]?"
    );
    private static final Pattern UNSUPPORTED_PROPERTY_CLAIM_PATTERN = Pattern.compile(
            "(?i)[^.!?\\n]*\\b(?:wi-?fi|wireless internet|audiovisual|\\bAV\\b|projector|screen|conference (?:hall|room|space)|meeting (?:hall|room|space)|restaurant|bar|spa|shuttle|airport transfer|parking|gym|air conditioning|laundry|plunge pool|swimming pool|balcony|panoramic? views?|estate views?|coffee tasting|plantation walk|guided walk|bonfire|bespoke catering|in-house catering|catering|lawn hire|lawn layouts?|banquet lawns?|banquet setups?|decor(?: guidelines)?|high-tea|lunch menus?|dinner menus?|day packages?)\\b[^.!?\\n]*[.!?]?"
    );

    public static String sanitize(String rawDraft) {
        if (rawDraft == null || rawDraft.isBlank()) {
            return QUOTE_BLOCK_PLACEHOLDER;
        }

        // 1. Sanitize unsupported hold assertions
        String sanitized = HOLD_PATTERN.matcher(rawDraft)
                .replaceAll("Please note that enquiries are informational and do not hold room allotment until a booking is formally created.");

        // 2. Sanitize unverified meal/activity inclusions
        sanitized = MEAL_INCLUSION_PATTERN.matcher(sanitized)
                .replaceAll("Inclusions and meal plans will be confirmed by reservations based on your selected rate plan.");

        // 3. Sanitize unsupported facility, equipment, and group activity claims
        sanitized = UNSUPPORTED_FACILITY_ACTIVITY_PATTERN.matcher(sanitized)
                .replaceAll("Our events coordinator will confirm equipment and activities directly based on your team's requirements.");

        // AI has no approved amenity catalogue in its request context; defer feature claims to staff.
        sanitized = UNSUPPORTED_PROPERTY_CLAIM_PATTERN.matcher(sanitized)
                .replaceAll("Our team will confirm property features, facilities, meals, and inclusions directly with you.");

        // 4. Replace any invented price patterns with placeholder
        sanitized = PRICE_PATTERN.matcher(sanitized).replaceAll(QUOTE_BLOCK_PLACEHOLDER);
        sanitized = BARE_RATE_CLAIM_PATTERN.matcher(sanitized).replaceAll(QUOTE_BLOCK_PLACEHOLDER);

        // Deduplicate consecutive placeholders if multiple matched
        while (sanitized.contains(QUOTE_BLOCK_PLACEHOLDER + " " + QUOTE_BLOCK_PLACEHOLDER) ||
               sanitized.contains(QUOTE_BLOCK_PLACEHOLDER + QUOTE_BLOCK_PLACEHOLDER)) {
            sanitized = sanitized.replace(QUOTE_BLOCK_PLACEHOLDER + " " + QUOTE_BLOCK_PLACEHOLDER, QUOTE_BLOCK_PLACEHOLDER);
            sanitized = sanitized.replace(QUOTE_BLOCK_PLACEHOLDER + QUOTE_BLOCK_PLACEHOLDER, QUOTE_BLOCK_PLACEHOLDER);
        }

        return sanitized;
    }
}
