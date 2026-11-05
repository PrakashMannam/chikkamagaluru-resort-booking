package com.companya.resort.service;

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
import com.companya.resort.util.DraftPriceSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuoteInjectorService {

    private final RoomCategoryRepository roomCategoryRepository;
    private final DailyInventoryRateRepository dailyInventoryRateRepository;
    private final BlackoutDateRepository blackoutDateRepository;
    private final RatePlanRepository ratePlanRepository;
    private final PricingService pricingService;
    private final ObjectMapper objectMapper;

    public record InjectedQuote(String finalDraftBody, String quotedRateBreakdownJson) {}

    public QuoteInjectorService(
            RoomCategoryRepository roomCategoryRepository,
            DailyInventoryRateRepository dailyInventoryRateRepository,
            BlackoutDateRepository blackoutDateRepository,
            RatePlanRepository ratePlanRepository,
            PricingService pricingService,
            ObjectMapper objectMapper
    ) {
        this.roomCategoryRepository = roomCategoryRepository;
        this.dailyInventoryRateRepository = dailyInventoryRateRepository;
        this.blackoutDateRepository = blackoutDateRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.pricingService = pricingService;
        this.objectMapper = objectMapper;
    }

    public InjectedQuote injectDatabaseQuote(String rawDraft, Enquiry enquiry) {
        String sanitizedDraft = DraftPriceSanitizer.sanitize(rawDraft);

        String quoteBlock;
        String breakdownJson = null;

        LocalDate checkIn = enquiry.getExtractedCheckIn();
        LocalDate checkOut = enquiry.getExtractedCheckOut();

        if (checkIn != null && checkOut == null) {
            int nights = enquiry.getExtractedNights() != null && enquiry.getExtractedNights() > 0 ? enquiry.getExtractedNights() : 1;
            checkOut = checkIn.plusDays(nights);
        }

        if (enquiry.getCategory() == EnquiryCategory.ROOM_BOOKING && checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) {
            // Determine category preference
            String raw = enquiry.getRawMessage() != null ? enquiry.getRawMessage().toLowerCase() : "";
            String categoryCode = raw.contains("villa") ? "VILLA" : "ROOM";

            RoomCategory roomCat = roomCategoryRepository.findByCode(categoryCode).orElse(null);
            if (roomCat != null) {
                // Check blackout dates
                List<BlackoutDate> blackouts = blackoutDateRepository.findConflictingBlackouts(roomCat.getId(), checkIn, checkOut);
                if (!blackouts.isEmpty()) {
                    quoteBlock = "Please note that our online allotment is fully committed for your requested dates. Our reservations team is currently reviewing house inventory for any possible openings.";
                    breakdownJson = createBlackoutBreakdownJson(categoryCode, checkIn, checkOut);
                } else {
                    List<DailyInventoryRate> rates = dailyInventoryRateRepository.findByRoomCategoryIdAndStayDateRange(roomCat.getId(), checkIn, checkOut);
                    long requiredNights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

                    if (rates.size() == requiredNights && !rates.isEmpty()) {
                        // Find active rate plan for the room category (prefer EP)
                        List<RatePlan> activePlans = ratePlanRepository.findByRoomCategoryIdAndIsActiveTrue(roomCat.getId());
                        RatePlan ratePlan = activePlans.stream()
                                .filter(p -> "EP".equalsIgnoreCase(p.getCode()))
                                .findFirst()
                                .orElseGet(() -> activePlans.isEmpty() ? null : activePlans.get(0));

                        if (ratePlan != null) {
                            int rooms = (enquiry.getExtractedRooms() != null && enquiry.getExtractedRooms() > 0)
                                    ? enquiry.getExtractedRooms()
                                    : 1;

                            // Authoritative pricing calculation via PricingService (including GST slab lookup and rate plan additions)
                            PriceCalculationResult priceResult = pricingService.calculatePrice(rates, ratePlan, rooms, null);

                            String nightsStr = requiredNights + " night" + (requiredNights > 1 ? "s" : "");
                            String roomsStr = rooms > 1 ? (", " + rooms + " rooms") : "";

                            quoteBlock = "The standard rate for the " + roomCat.getName() + " under the " + ratePlan.getName() +
                                    " plan for your dates (" + checkIn + " to " + checkOut + ", " + nightsStr + roomsStr +
                                    ") is ₹" + priceResult.getTotalPayableInr().toPlainString() +
                                    " inclusive of applicable taxes (₹" + priceResult.getSubtotalBeforeDiscountInr().toPlainString() +
                                    " base tariff + ₹" + priceResult.getGstAmountInr().toPlainString() +
                                    " GST at " + priceResult.getGstPercentageApplied().toPlainString() + "%).";

                            breakdownJson = createAvailableBreakdownJson(
                                    categoryCode, ratePlan.getCode(), checkIn, checkOut, (int) requiredNights, rooms,
                                    priceResult.getSubtotalBeforeDiscountInr(), priceResult.getGstPercentageApplied(),
                                    priceResult.getGstAmountInr(), priceResult.getTotalPayableInr(), priceResult.getGstVerificationStatus()
                            );
                        } else {
                            quoteBlock = "Our reservations team will confirm the current rates and availability for your requested stay.";
                        }
                    } else {
                        quoteBlock = "Our reservations team will confirm the current rates and availability for your requested stay.";
                    }
                }
            } else {
                quoteBlock = "Our reservations team will confirm the current rates and availability for your requested stay.";
            }
        } else {
            // Weddings, offsites, missing dates, complaints or unsorted enquiries
            quoteBlock = "Our reservations team will confirm the current rates and availability for your requested stay.";
        }

        String finalBody;
        if (sanitizedDraft.contains(DraftPriceSanitizer.QUOTE_BLOCK_PLACEHOLDER)) {
            finalBody = sanitizedDraft.replace(DraftPriceSanitizer.QUOTE_BLOCK_PLACEHOLDER, quoteBlock);
        } else {
            finalBody = sanitizedDraft + "\n\n" + quoteBlock;
        }

        return new InjectedQuote(finalBody, breakdownJson);
    }

    private String createAvailableBreakdownJson(
            String categoryCode, String ratePlanCode, LocalDate checkIn, LocalDate checkOut,
            int nights, int rooms, BigDecimal preTaxSubtotal, BigDecimal gstPercentage,
            BigDecimal gstAmount, BigDecimal grandTotal, String gstStatus
    ) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("room_category_code", categoryCode);
            map.put("rate_plan_code", ratePlanCode);
            map.put("check_in", checkIn.toString());
            map.put("check_out", checkOut.toString());
            map.put("nights", nights);
            map.put("rooms", rooms);
            map.put("pre_tax_subtotal", preTaxSubtotal);
            map.put("gst_percentage", gstPercentage);
            map.put("gst_amount", gstAmount);
            map.put("grand_total", grandTotal);
            map.put("gst_status", gstStatus);
            map.put("allotment_status", "AVAILABLE");
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }

    private String createBlackoutBreakdownJson(String categoryCode, LocalDate checkIn, LocalDate checkOut) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("room_category_code", categoryCode);
            map.put("check_in", checkIn.toString());
            map.put("check_out", checkOut.toString());
            map.put("allotment_status", "BLACKOUT_DATES");
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }
}
