package com.companya.resort;

import com.companya.resort.dto.GeminiEnquiryExtraction;
import com.companya.resort.service.GeminiAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Tag("live-api")
public class GeminiLiveApiSmokeTest {

    @Test
    @DisplayName("Opt-in live Gemini API smoke test using gemini-3.8-flash and x-goog-api-key header")
    void testLiveGeminiApiStructuredOutput() {
        String liveFlag = System.getenv("LIVE_GEMINI_TEST");
        if (liveFlag == null || liveFlag.isBlank()) {
            liveFlag = System.getProperty("live.gemini.test");
        }

        Assumptions.assumeTrue(
                "true".equalsIgnoreCase(liveFlag),
                "Skipping live Gemini smoke test: LIVE_GEMINI_TEST is not set to 'true'."
        );

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getProperty("gemini.api.key");
        }

        Assumptions.assumeTrue(
                apiKey != null && !apiKey.isBlank() && !apiKey.toLowerCase().contains("dummy"),
                "Skipping live Gemini smoke test: GEMINI_API_KEY environment variable is not configured with a valid key."
        );

        String model = System.getenv("GEMINI_MODEL");
        if (model == null || model.isBlank()) {
            model = System.getProperty("gemini.model", "gemini-3.8-flash");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        GeminiAiService service = new GeminiAiService(apiKey, model, objectMapper);

        // Minimal synthetic enquiry with NO real PII
        String syntheticEnquiry = "Is a luxury villa free from 2027-02-14 to 2027-02-16 for 2 adults?";

        GeminiAiService.AiProcessingResult result = service.processEnquiry(syntheticEnquiry);

        assertNotNull(result, "Result from Gemini should not be null");
        assertTrue(result.isSuccess(), "Live Gemini call should succeed; error=" + result.errorStatus() + ": " + result.errorMessage());

        GeminiEnquiryExtraction extraction = result.extraction();
        assertNotNull(extraction, "Structured extraction must not be null");
        assertNotNull(extraction.getCategory(), "Extracted category must not be null");
        assertEquals("ROOM_BOOKING", extraction.getCategory());

        assertNotNull(extraction.getConfidence(), "Confidence must be present");
        assertTrue(extraction.getConfidence().compareTo(BigDecimal.ZERO) > 0, "Confidence must be greater than zero");

        assertNotNull(extraction.getSummary(), "Summary must be present");
        assertFalse(extraction.getSummary().isBlank(), "Summary must not be empty");

        assertNotNull(extraction.getSuggestedReply(), "Suggested reply draft must be present");
        assertFalse(extraction.getSuggestedReply().isBlank(), "Suggested reply draft must not be empty");
    }
}
