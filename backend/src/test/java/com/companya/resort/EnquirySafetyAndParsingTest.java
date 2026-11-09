package com.companya.resort;

import com.companya.resort.dto.PriceCalculationResult;
import com.companya.resort.entity.BlackoutDate;
import com.companya.resort.entity.DailyInventoryRate;
import com.companya.resort.entity.Enquiry;
import com.companya.resort.entity.EnquiryCategory;
import com.companya.resort.entity.RatePlan;
import com.companya.resort.entity.RoomCategory;
import com.companya.resort.repository.BlackoutDateRepository;
import com.companya.resort.repository.DailyInventoryRateRepository;
import com.companya.resort.repository.RatePlanRepository;
import com.companya.resort.repository.RoomCategoryRepository;
import com.companya.resort.service.PricingService;
import com.companya.resort.service.QuoteInjectorService;
import com.companya.resort.util.BudgetValidator;
import com.companya.resort.util.DraftPriceSanitizer;
import com.companya.resort.util.PiiRedactor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnquirySafetyAndParsingTest {

    @Mock
    private RoomCategoryRepository roomCategoryRepository;

    @Mock
    private DailyInventoryRateRepository dailyInventoryRateRepository;

    @Mock
    private BlackoutDateRepository blackoutDateRepository;

    @Mock
    private RatePlanRepository ratePlanRepository;

    @Mock
    private PricingService pricingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("PII Redactor should mask contact details and common government ID formats before sending to AI")
    void testPiiRedactor() {
        String input = "Synthetic guest: guest.qa@example.test, mobile +91 60000 00000, landline 08262-000000, intl +1 555-010-0100, Aadhaar 0000 0000 0000, PAN AAAAA0000A, passport P0000000.";
        String redacted = PiiRedactor.redact(input);

        assertFalse(redacted.contains("guest.qa@example.test"));
        assertFalse(redacted.contains("60000 00000"));
        assertFalse(redacted.contains("08262-000000"));
        assertFalse(redacted.contains("555-010-0100"));
        assertFalse(redacted.contains("0000 0000 0000"));
        assertFalse(redacted.contains("AAAAA0000A"));
        assertFalse(redacted.contains("P0000000"));
        assertTrue(redacted.contains("[REDACTED_EMAIL]"));
        assertTrue(redacted.contains("[REDACTED_PHONE]"));
        assertTrue(redacted.contains("[REDACTED_ID]"));
    }

    @Test
    @DisplayName("BudgetValidator should extract explicit budgets and discard ambiguous standalone numbers")
    void testBudgetValidator() {
        // Explicit INR budgets
        assertTrue(BudgetValidator.containsExplicitBudget("Our budget is Rs. 50,000 for 2 nights"));
        assertEquals(new BigDecimal("50000.00"), BudgetValidator.extractBudgetIfExplicit("Our budget is Rs. 50,000 for 2 nights", null));

        assertTrue(BudgetValidator.containsExplicitBudget("Budget around 2 Lakhs"));
        assertEquals(new BigDecimal("200000.00"), BudgetValidator.extractBudgetIfExplicit("Budget around 2 Lakhs", null));

        assertTrue(BudgetValidator.containsExplicitBudget("We have ₹75,000 in mind"));
        assertEquals(new BigDecimal("75000.00"), BudgetValidator.extractBudgetIfExplicit("We have ₹75,000 in mind", null));

        // Ambiguous guest or room numbers without currency symbol must be rejected
        assertFalse(BudgetValidator.containsExplicitBudget("We are a group of 50 people needing 20 rooms for 2 nights"));
        assertNull(BudgetValidator.extractBudgetIfExplicit("We are a group of 50 people needing 20 rooms", new BigDecimal("50")));
    }

    @Test
    @DisplayName("DraftPriceSanitizer should replace hallucinated AI prices with placeholder")
    void testDraftPriceSanitizer() {
        String aiDraft = "Dear Guest, We can offer the Luxury Villa for ₹14,500 per night, plus 18% GST. Total is Rs 29,000.";
        String sanitized = DraftPriceSanitizer.sanitize(aiDraft);

        assertFalse(sanitized.contains("₹14,500"));
        assertFalse(sanitized.contains("29,000"));
        assertTrue(sanitized.contains("{{RATE_TABLE_QUOTE_BLOCK}}"));
    }

    @Test
    @DisplayName("DraftPriceSanitizer removes bare numeric rate claims but preserves party sizes")
    void testBareNumericRateClaimSanitizer() {
        String sanitized = DraftPriceSanitizer.sanitize("For 2 rooms and 4 adults, the nightly rate is 14500 per night.");
        assertFalse(sanitized.contains("14500"));
        assertTrue(sanitized.contains("2 rooms"));
        assertTrue(sanitized.contains("4 adults"));
        assertTrue(sanitized.contains("{{RATE_TABLE_QUOTE_BLOCK}}"));
    }

    @Test
    @DisplayName("QuoteInjectorService should delegate to PricingService using DB rates, RatePlan, and GST slab calculation")
    void testQuoteInjectorServiceWithAuthoritativePricing() {
        QuoteInjectorService service = new QuoteInjectorService(
                roomCategoryRepository, dailyInventoryRateRepository, blackoutDateRepository,
                ratePlanRepository, pricingService, objectMapper
        );

        RoomCategory villa = new RoomCategory();
        villa.setId(2L);
        villa.setCode("VILLA");
        villa.setName("Jacuzzi Villa");

        when(roomCategoryRepository.findByCode("VILLA")).thenReturn(Optional.of(villa));
        when(blackoutDateRepository.findConflictingBlackouts(eq(2L), any(), any())).thenReturn(Collections.emptyList());

        DailyInventoryRate rate1 = new DailyInventoryRate();
        rate1.setBaseRateInr(new BigDecimal("12000.00"));
        DailyInventoryRate rate2 = new DailyInventoryRate();
        rate2.setBaseRateInr(new BigDecimal("12000.00"));

        LocalDate checkIn = LocalDate.of(2027, 2, 10);
        LocalDate checkOut = LocalDate.of(2027, 2, 12);

        when(dailyInventoryRateRepository.findByRoomCategoryIdAndStayDateRange(eq(2L), eq(checkIn), eq(checkOut)))
                .thenReturn(List.of(rate1, rate2));

        RatePlan epPlan = new RatePlan();
        epPlan.setId(10L);
        epPlan.setCode("EP");
        epPlan.setName("European Plan (Room Only)");
        when(ratePlanRepository.findByRoomCategoryIdAndIsActiveTrue(2L)).thenReturn(List.of(epPlan));

        PriceCalculationResult priceResult = new PriceCalculationResult(
                new BigDecimal("24000.00"),
                BigDecimal.ZERO,
                new BigDecimal("24000.00"),
                new BigDecimal("18.00"),
                new BigDecimal("4320.00"),
                new BigDecimal("28320.00"),
                "VERIFIED",
                null
        );
        when(pricingService.calculatePrice(anyList(), eq(epPlan), eq(1), isNull())).thenReturn(priceResult);

        Enquiry enquiry = new Enquiry();
        enquiry.setCategory(EnquiryCategory.ROOM_BOOKING);
        enquiry.setRawMessage("Looking for a villa from 10 Feb to 12 Feb 2027");
        enquiry.setExtractedCheckIn(checkIn);
        enquiry.setExtractedCheckOut(checkOut);
        enquiry.setExtractedNights(2);

        String rawDraft = "Dear Guest, Thank you for contacting us.\n\n{{RATE_TABLE_QUOTE_BLOCK}}\n\nBest regards.";
        QuoteInjectorService.InjectedQuote result = service.injectDatabaseQuote(rawDraft, enquiry);

        assertNotNull(result);
        verify(pricingService).calculatePrice(anyList(), eq(epPlan), eq(1), isNull());
        assertTrue(result.finalDraftBody().contains("₹28320.00 inclusive of applicable taxes"));
        assertTrue(result.finalDraftBody().contains("₹24000.00 base tariff"));
        assertTrue(result.finalDraftBody().contains("₹4320.00 GST at 18.00%"));
        assertTrue(result.finalDraftBody().contains("Jacuzzi Villa"));
        assertNotNull(result.quotedRateBreakdownJson());
        assertTrue(result.quotedRateBreakdownJson().contains("28320.00"));
        assertTrue(result.quotedRateBreakdownJson().contains("AVAILABLE"));
        assertTrue(result.quotedRateBreakdownJson().contains("\"gst_status\":\"VERIFIED\""));
    }

    @Test
    @DisplayName("QuoteInjectorService should not claim confirmed GST when pricing snapshot status is UNVERIFIED")
    void testQuoteInjectorServiceWithUnverifiedGst() {
        QuoteInjectorService service = new QuoteInjectorService(
                roomCategoryRepository, dailyInventoryRateRepository, blackoutDateRepository,
                ratePlanRepository, pricingService, objectMapper
        );

        RoomCategory cottage = new RoomCategory();
        cottage.setId(1L);
        cottage.setCode("ROOM");
        cottage.setName("Coffee Cottage");

        when(roomCategoryRepository.findByCode("ROOM")).thenReturn(Optional.of(cottage));
        when(blackoutDateRepository.findConflictingBlackouts(eq(1L), any(), any())).thenReturn(Collections.emptyList());

        DailyInventoryRate rate1 = new DailyInventoryRate();
        rate1.setBaseRateInr(new BigDecimal("9500.00"));
        DailyInventoryRate rate2 = new DailyInventoryRate();
        rate2.setBaseRateInr(new BigDecimal("9500.00"));

        LocalDate checkIn = LocalDate.of(2026, 11, 14);
        LocalDate checkOut = LocalDate.of(2026, 11, 16);

        when(dailyInventoryRateRepository.findByRoomCategoryIdAndStayDateRange(eq(1L), eq(checkIn), eq(checkOut)))
                .thenReturn(List.of(rate1, rate2));

        RatePlan epPlan = new RatePlan();
        epPlan.setId(1L);
        epPlan.setCode("EP");
        epPlan.setName("European Plan (Room Only)");
        when(ratePlanRepository.findByRoomCategoryIdAndIsActiveTrue(1L)).thenReturn(List.of(epPlan));

        PriceCalculationResult priceResult = new PriceCalculationResult(
                new BigDecimal("19000.00"),
                BigDecimal.ZERO,
                new BigDecimal("19000.00"),
                new BigDecimal("12.00"),
                new BigDecimal("2280.00"),
                new BigDecimal("21280.00"),
                "UNVERIFIED",
                null
        );
        when(pricingService.calculatePrice(anyList(), eq(epPlan), eq(1), isNull())).thenReturn(priceResult);

        Enquiry enquiry = new Enquiry();
        enquiry.setCategory(EnquiryCategory.ROOM_BOOKING);
        enquiry.setRawMessage("Looking for a room from 14 Nov to 16 Nov 2026");
        enquiry.setExtractedCheckIn(checkIn);
        enquiry.setExtractedCheckOut(checkOut);
        enquiry.setExtractedNights(2);

        String rawDraft = "Dear Guest, Thank you for contacting us.\n\n{{RATE_TABLE_QUOTE_BLOCK}}\n\nBest regards.";
        QuoteInjectorService.InjectedQuote result = service.injectDatabaseQuote(rawDraft, enquiry);

        assertNotNull(result);
        verify(pricingService).calculatePrice(anyList(), eq(epPlan), eq(1), isNull());

        // 1. Must NOT claim taxes are confirmed or that total is inclusive of taxes
        assertFalse(result.finalDraftBody().contains("inclusive of applicable taxes"),
                "Unverified draft must not claim total is inclusive of applicable taxes");
        assertFalse(result.finalDraftBody().contains("confirmed"),
                "Unverified draft must not imply taxes are confirmed");

        // 2. Must label tax as provisional and pending property verification
        assertTrue(result.finalDraftBody().contains("₹19000.00 base tariff"),
                "Draft must present base tariff explicitly");
        assertTrue(result.finalDraftBody().contains("applicable GST of ₹2280.00 at 12.00% is provisional and pending property verification"),
                "Draft must label tax amount as provisional and pending property verification");
        assertTrue(result.finalDraftBody().contains("provisional total: ₹21280.00"),
                "Total must be qualified as provisional");
        assertTrue(result.finalDraftBody().contains("Coffee Cottage"));

        // 3. Must preserve breakdown JSON with UNVERIFIED status
        assertNotNull(result.quotedRateBreakdownJson());
        assertTrue(result.quotedRateBreakdownJson().contains("\"gst_status\":\"UNVERIFIED\""));
        assertTrue(result.quotedRateBreakdownJson().contains("21280.00"));
    }

    @Test
    @DisplayName("QuoteInjectorService should safely treat null GST status as unverified")
    void testQuoteInjectorServiceWithNullGstStatusDefaultsToUnverified() {
        QuoteInjectorService service = new QuoteInjectorService(
                roomCategoryRepository, dailyInventoryRateRepository, blackoutDateRepository,
                ratePlanRepository, pricingService, objectMapper
        );

        RoomCategory cottage = new RoomCategory();
        cottage.setId(1L);
        cottage.setCode("ROOM");
        cottage.setName("Coffee Cottage");

        when(roomCategoryRepository.findByCode("ROOM")).thenReturn(Optional.of(cottage));
        when(blackoutDateRepository.findConflictingBlackouts(eq(1L), any(), any())).thenReturn(Collections.emptyList());

        DailyInventoryRate rate1 = new DailyInventoryRate();
        rate1.setBaseRateInr(new BigDecimal("9500.00"));

        LocalDate checkIn = LocalDate.of(2026, 11, 14);
        LocalDate checkOut = LocalDate.of(2026, 11, 15);

        when(dailyInventoryRateRepository.findByRoomCategoryIdAndStayDateRange(eq(1L), eq(checkIn), eq(checkOut)))
                .thenReturn(List.of(rate1));

        RatePlan epPlan = new RatePlan();
        epPlan.setId(1L);
        epPlan.setCode("EP");
        epPlan.setName("European Plan (Room Only)");
        when(ratePlanRepository.findByRoomCategoryIdAndIsActiveTrue(1L)).thenReturn(List.of(epPlan));

        PriceCalculationResult priceResult = new PriceCalculationResult(
                new BigDecimal("9500.00"),
                BigDecimal.ZERO,
                new BigDecimal("9500.00"),
                new BigDecimal("12.00"),
                new BigDecimal("1140.00"),
                new BigDecimal("10640.00"),
                null,
                null
        );
        when(pricingService.calculatePrice(anyList(), eq(epPlan), eq(1), isNull())).thenReturn(priceResult);

        Enquiry enquiry = new Enquiry();
        enquiry.setCategory(EnquiryCategory.ROOM_BOOKING);
        enquiry.setRawMessage("Looking for 1 night stay 14 Nov 2026");
        enquiry.setExtractedCheckIn(checkIn);
        enquiry.setExtractedCheckOut(checkOut);
        enquiry.setExtractedNights(1);

        String rawDraft = "{{RATE_TABLE_QUOTE_BLOCK}}";
        QuoteInjectorService.InjectedQuote result = service.injectDatabaseQuote(rawDraft, enquiry);

        assertNotNull(result);
        assertFalse(result.finalDraftBody().contains("inclusive of applicable taxes"));
        assertTrue(result.finalDraftBody().contains("is provisional and pending property verification"));
        assertTrue(result.finalDraftBody().contains("provisional total: ₹10640.00"));
    }

    @Test
    @DisplayName("QuoteInjectorService should inject blackout notice if dates are blocked in DB")
    void testQuoteInjectorServiceBlackoutNotice() {
        QuoteInjectorService service = new QuoteInjectorService(
                roomCategoryRepository, dailyInventoryRateRepository, blackoutDateRepository,
                ratePlanRepository, pricingService, objectMapper
        );

        RoomCategory villa = new RoomCategory();
        villa.setId(2L);
        villa.setCode("VILLA");
        villa.setName("Jacuzzi Villa");

        when(roomCategoryRepository.findByCode("VILLA")).thenReturn(Optional.of(villa));

        BlackoutDate blackout = new BlackoutDate();
        blackout.setReason("Fully Booked Estate Event");
        when(blackoutDateRepository.findConflictingBlackouts(eq(2L), any(), any())).thenReturn(List.of(blackout));

        LocalDate checkIn = LocalDate.of(2027, 2, 10);
        LocalDate checkOut = LocalDate.of(2027, 2, 12);

        Enquiry enquiry = new Enquiry();
        enquiry.setCategory(EnquiryCategory.ROOM_BOOKING);
        enquiry.setRawMessage("Looking for a villa from 10 Feb to 12 Feb 2027");
        enquiry.setExtractedCheckIn(checkIn);
        enquiry.setExtractedCheckOut(checkOut);

        String rawDraft = "Dear Guest,\n\n{{RATE_TABLE_QUOTE_BLOCK}}\n\nBest regards.";
        QuoteInjectorService.InjectedQuote result = service.injectDatabaseQuote(rawDraft, enquiry);

        assertTrue(result.finalDraftBody().contains("online allotment is fully committed"));
        assertTrue(result.quotedRateBreakdownJson().contains("BLACKOUT_DATES"));
    }

    @Test
    @DisplayName("RuleFallbackService: Detects clear complaints accurately")
    void testRuleFallbackService_DetectsComplaints() {
        com.companya.resort.service.RuleFallbackService fallbackService = new com.companya.resort.service.RuleFallbackService();

        // 1. Service failure and ruined event
        String complaint1 = "I am deeply disappointed with the hot water failure during our stay in villa 3 yesterday. This is completely unacceptable for a luxury property and ruined our anniversary.";
        var ext1 = fallbackService.classifyFallback(complaint1);
        assertEquals("COMPLAINT", ext1.getCategory());
        assertEquals(new BigDecimal("0.85"), ext1.getConfidence());
        assertTrue(ext1.getSuggestedReply().contains("apologize"));

        // 2. Rude staff / terrible experience
        String complaint2 = "Worst experience ever, the staff was extremely rude and dismissive.";
        var ext2 = fallbackService.classifyFallback(complaint2);
        assertEquals("COMPLAINT", ext2.getCategory());

        // 3. Dirty room / unhygienic
        String complaint3 = "Terrible service, room was dirty and bedsheet was filthy.";
        var ext3 = fallbackService.classifyFallback(complaint3);
        assertEquals("COMPLAINT", ext3.getCategory());
    }

    @Test
    @DisplayName("RuleFallbackService: Ordinary policy inquiries and ambiguous queries are NOT classified as complaints")
    void testRuleFallbackService_OrdinaryInquiriesAreNotComplaints() {
        com.companya.resort.service.RuleFallbackService fallbackService = new com.companya.resort.service.RuleFallbackService();

        // Policy query asking about refund policy
        String query1 = "Can you share what is your refund policy and cancellation terms for monsoon stays?";
        var ext1 = fallbackService.classifyFallback(query1);
        assertNotEquals("COMPLAINT", ext1.getCategory());

        // Simple ambiguous message
        String query2 = "Hello, I am interested in visiting next month. Can you provide contact info?";
        var ext2 = fallbackService.classifyFallback(query2);
        assertEquals("UNSORTED", ext2.getCategory());
        assertNotEquals("COMPLAINT", ext2.getCategory());
    }
}
