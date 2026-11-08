package com.companya.resort.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DraftPriceSanitizerTest {

    @Test
    @DisplayName("Null or blank input returns quote block placeholder")
    void testNullOrBlank() {
        assertEquals("{{RATE_TABLE_QUOTE_BLOCK}}", DraftPriceSanitizer.sanitize(null));
        assertEquals("{{RATE_TABLE_QUOTE_BLOCK}}", DraftPriceSanitizer.sanitize(""));
        assertEquals("{{RATE_TABLE_QUOTE_BLOCK}}", DraftPriceSanitizer.sanitize("   "));
    }

    @Test
    @DisplayName("Invented room rates with INR, Rs, and currency words are sanitized to quote block")
    void testPriceSanitization() {
        String input = "We can offer the Private Pool Villa for ₹15,000 per night or Rs. 12500 only.";
        String result = DraftPriceSanitizer.sanitize(input);
        assertFalse(result.contains("15,000"));
        assertFalse(result.contains("12500"));
        assertTrue(result.contains("{{RATE_TABLE_QUOTE_BLOCK}}"));
    }

    @Test
    @DisplayName("Unsupported hold assertions are sanitized with informational disclaimer")
    void testHoldAssertionsSanitized() {
        String input = "Dear Dr. Swaminathan, We have held a Private Pool Villa for your dates. Warm regards.";
        String result = DraftPriceSanitizer.sanitize(input);
        assertFalse(result.toLowerCase().contains("we have held"));
        assertTrue(result.contains("enquiries are informational and do not hold room allotment"));
    }

    @Test
    @DisplayName("Unverified meal and activity inclusions are sanitized with reservations disclaimer")
    void testMealInclusionsSanitized() {
        String input = "The stay includes gourmet breakfast and a complimentary plantation walk.";
        String result = DraftPriceSanitizer.sanitize(input);
        assertFalse(result.toLowerCase().contains("includes gourmet breakfast"));
        assertFalse(result.toLowerCase().contains("complimentary plantation walk"));
        assertTrue(result.contains("Inclusions and meal plans will be confirmed by reservations"));
    }

    @Test
    @DisplayName("Unsupported facility, AV equipment, and group activity claims are sanitized with coordinator disclaimer")
    void testFacilityAndActivityClaimsSanitized() {
        String input = "Our conference pavilion features AV projection, high-speed WiFi, and panoramic plantation views, paired with guided cupping sessions.";
        String result = DraftPriceSanitizer.sanitize(input);
        assertFalse(result.contains("AV projection"));
        assertFalse(result.contains("high-speed WiFi"));
        assertFalse(result.contains("panoramic plantation views"));
        assertFalse(result.contains("guided cupping sessions"));
        assertTrue(result.contains("Our events coordinator will confirm equipment and activities directly"));
    }

    @Test
    @DisplayName("Unverified Wi-Fi and meeting-room claims are deferred even with alternate wording")
    void testAlternateAmenityClaimsSanitized() {
        String result = DraftPriceSanitizer.sanitize("The meeting room includes Wi-Fi and a large projection screen.");
        assertFalse(result.toLowerCase().contains("meeting room includes"));
        assertFalse(result.toLowerCase().contains("wi-fi"));
        assertTrue(result.contains("Our team will confirm property features, facilities, meals, and inclusions directly with you."));
    }

    @Test
    @DisplayName("Consecutive sanitized placeholders are deduplicated")
    void testPlaceholderDeduplication() {
        String input = "Tariff is ₹10,000 or ₹12,000.";
        String result = DraftPriceSanitizer.sanitize(input);
        assertFalse(result.contains("{{RATE_TABLE_QUOTE_BLOCK}} {{RATE_TABLE_QUOTE_BLOCK}}"));
        assertFalse(result.contains("{{RATE_TABLE_QUOTE_BLOCK}}{{RATE_TABLE_QUOTE_BLOCK}}"));
    }
}
