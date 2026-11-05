package com.companya.resort.service;

import com.companya.resort.dto.GeminiEnquiryExtraction;
import com.companya.resort.entity.EnquiryCategory;
import com.companya.resort.util.BudgetValidator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RuleFallbackService {

    private static final Pattern COMPLAINT_PATTERN = Pattern.compile(
            "\\b(worst|terrible|horrible|unacceptable|pathetic|disaster|disgusting|ruined|furious|cheated|rude|dirty|filthy|bekaar|ganda|shikayat)\\b" +
            "|\\b(disappointed|disappointing|disappointment)\\b" +
            "|\\b(poor\\s+service|bad\\s+service|horrible\\s+experience|terrible\\s+experience|awful\\s+stay)\\b" +
            "|\\b(file\\s+a\\s+complaint|filing\\s+a\\s+complaint|formal\\s+complaint|official\\s+complaint|register\\s+a\\s+complaint|raise\\s+a\\s+complaint|demand\\s+a\\s+refund|want\\s+a\\s+refund|full\\s+refund|money\\s+back)\\b" +
            "|\\b(ruined\\s+our|water\\s+failure|power\\s+failure|hot\\s+water\\s+failure)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern POLICY_INQUIRY_PATTERN = Pattern.compile(
            "\\b(refund\\s+policy|cancellation\\s+policy|is\\s+it\\s+refundable|refundable\\s+deposit|policy\\s+on\\s+refunds?)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern WEDDING_PATTERN = Pattern.compile(
            "\\b(wedding|shaadi|shadi|bride|groom|baraat|mehendi|sangeet|mandap|haldi)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern OFFSITE_PATTERN = Pattern.compile(
            "\\b(offsite|corporate|conference|team of|annual meet|board meeting|townhall|workshop)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ROOM_PATTERN = Pattern.compile(
            "\\b(room|villa|stay|book|tariff|couple|adults|night|check-in|vacant|free|available|kamra)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GUESTS_PATTERN = Pattern.compile(
            "(?:(\\d+)\\s*(?:guests?|pax|adults?)|team\\s+of\\s+(\\d+))",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern NIGHTS_PATTERN = Pattern.compile(
            "(\\d+)\\s*nights?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ROOMS_PATTERN = Pattern.compile(
            "(\\d+)\\s*(?:rooms?|villas?|keys?)",
            Pattern.CASE_INSENSITIVE
    );

    public GeminiEnquiryExtraction classifyFallback(String rawMessage) {
        GeminiEnquiryExtraction extraction = new GeminiEnquiryExtraction();
        if (rawMessage == null || rawMessage.isBlank()) {
            extraction.setCategory(EnquiryCategory.UNSORTED.name());
            extraction.setConfidence(BigDecimal.ZERO);
            extraction.setSummary("Empty enquiry");
            extraction.setSuggestedReply("Thank you for contacting us. Our team has received your message and will get back to you shortly.");
            return extraction;
        }

        // 1. Complaint Check (Highest Priority)
        if (COMPLAINT_PATTERN.matcher(rawMessage).find()) {
            extraction.setCategory(EnquiryCategory.COMPLAINT.name());
            extraction.setConfidence(new BigDecimal("0.85"));
            extraction.setSummary("Guest complaint or service issue reported");
            extraction.setSuggestedReply("Dear Guest,\n\nWe sincerely apologize for your experience. Your feedback has been escalated directly to our General Manager, who will personally follow up with you.\n\nWarm regards,\nManagement Team");
            return extraction;
        }

        // 2. Specialty lanes: Wedding and Offsite take precedence over generic Room inquiries
        boolean isWedding = WEDDING_PATTERN.matcher(rawMessage).find();
        boolean isOffsite = OFFSITE_PATTERN.matcher(rawMessage).find();
        boolean isRoom = ROOM_PATTERN.matcher(rawMessage).find();

        if (isWedding && !isOffsite) {
            extraction.setCategory(EnquiryCategory.WEDDING.name());
            extraction.setConfidence(new BigDecimal("0.80"));
            extraction.setSummary("Guest enquiry for wedding and events");
            extraction.setSuggestedReply("Dear Guest,\n\nThank you for considering our estate for your wedding celebration. Our wedding specialist will review your details and connect with you shortly.\n\nWarm regards,\nWeddings Team");
        } else if (isOffsite && !isWedding) {
            extraction.setCategory(EnquiryCategory.CORPORATE_OFFSITE.name());
            extraction.setConfidence(new BigDecimal("0.80"));
            extraction.setSummary("Guest enquiry for corporate offsite and conferences");
            extraction.setSuggestedReply("Dear Guest,\n\nThank you for reaching out regarding your corporate offsite. Our corporate events specialist will contact you with package options.\n\nWarm regards,\nCorporate Events Team");
        } else if (isRoom && !isWedding && !isOffsite) {
            extraction.setCategory(EnquiryCategory.ROOM_BOOKING.name());
            extraction.setConfidence(new BigDecimal("0.75"));
            extraction.setSummary("Guest enquiry for room booking");
            extraction.setSuggestedReply("Dear Guest,\n\nThank you for reaching out to us. We have received your booking request and our reservations team will get in touch with you shortly with tariff and availability.\n\nWarm regards,\nReservations Team");
        } else {
            // Ambiguous (e.g. both wedding & offsite, or zero matches) -> UNSORTED
            extraction.setCategory(EnquiryCategory.UNSORTED.name());
            extraction.setConfidence(new BigDecimal("0.40"));
            extraction.setSummary("Unclassified enquiry requiring manual staff review");
            extraction.setSuggestedReply("Dear Guest,\n\nThank you for reaching out to us. We have received your enquiry and our team will get in touch with you shortly.\n\nWarm regards,\nEstate Team");
        }

        // Extract numbers if present
        Matcher guestMatcher = GUESTS_PATTERN.matcher(rawMessage);
        if (guestMatcher.find()) {
            String g = guestMatcher.group(1) != null ? guestMatcher.group(1) : guestMatcher.group(2);
            if (g != null) {
                try {
                    extraction.setGuests(Integer.parseInt(g));
                } catch (NumberFormatException ignored) {}
            }
        }

        Matcher nightsMatcher = NIGHTS_PATTERN.matcher(rawMessage);
        if (nightsMatcher.find()) {
            try {
                extraction.setNights(Integer.parseInt(nightsMatcher.group(1)));
            } catch (NumberFormatException ignored) {}
        }

        Matcher roomsMatcher = ROOMS_PATTERN.matcher(rawMessage);
        if (roomsMatcher.find()) {
            try {
                extraction.setRooms(Integer.parseInt(roomsMatcher.group(1)));
            } catch (NumberFormatException ignored) {}
        }

        if (BudgetValidator.containsExplicitBudget(rawMessage)) {
            extraction.setBudgetAmount(BudgetValidator.extractBudgetIfExplicit(rawMessage, null));
            extraction.setBudgetCurrency("INR");
        }

        extraction.setSuggestedReply("Dear Guest,\n\nThank you for reaching out to us. We have received your enquiry regarding " +
                extraction.getSummary().toLowerCase() + ". {{RATE_TABLE_QUOTE_BLOCK}}\n\nOur team is reviewing the details and will get in touch with you shortly.\n\nWarm regards,\nEstate Team");

        return extraction;
    }
}
