package com.companya.resort.service;

import com.companya.resort.dto.GeminiEnquiryExtraction;
import com.companya.resort.entity.EnquiryCategory;
import com.companya.resort.util.BudgetValidator;
import com.companya.resort.util.EventAccommodationDetector;
import com.companya.resort.util.EventAccommodationDetector.AccommodationRequirement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
            "(?:(\\d+)\\s*(?:guests?|pax|adults?|people|members?)|team\\s+of\\s+(\\d+))",
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

    private static final Pattern DATE_HINT_PATTERN = Pattern.compile(
            "(?i)\\b(?:in|on|for)\\s+((?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\\s+\\d{4})?|\\d{1,2}(?:st|nd|rd|th)?\\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*(?:\\s+\\d{4})?|\\d{1,2}[-/]\\d{1,2}(?:[-/]\\d{2,4})?)\\b"
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

        String dateHint = null;
        Matcher dateMatcher = DATE_HINT_PATTERN.matcher(rawMessage);
        if (dateMatcher.find()) {
            dateHint = dateMatcher.group(1).trim();
        }

        // 1. Complaint Check (Highest Priority)
        if (COMPLAINT_PATTERN.matcher(rawMessage).find()) {
            extraction.setCategory(EnquiryCategory.COMPLAINT.name());
            extraction.setConfidence(new BigDecimal("0.85"));
            extraction.setSummary("Guest complaint or service issue reported");
            extraction.setSuggestedReply("Dear Guest,\n\nWe sincerely apologize for your experience. We take guest feedback very seriously, and your message has been escalated directly to our General Manager, who will personally follow up with you.\n\nWarm regards,\nManagement Team");
            return extraction;
        }

        // 2. Specialty lanes: Wedding and Offsite take precedence over generic Room inquiries
        boolean isWedding = WEDDING_PATTERN.matcher(rawMessage).find();
        boolean isOffsite = OFFSITE_PATTERN.matcher(rawMessage).find();
        boolean isRoom = ROOM_PATTERN.matcher(rawMessage).find();

        if (isWedding && !isOffsite) {
            extraction.setCategory(EnquiryCategory.WEDDING.name());
            extraction.setConfidence(new BigDecimal("0.80"));
            AccommodationRequirement req = EventAccommodationDetector.detect(rawMessage);
            extraction.setAccommodationRequirement(req.name());

            Integer guests = extraction.getGuests();
            Integer nights = extraction.getNights();

            if (req == AccommodationRequirement.NOT_REQUIRED) {
                extraction.setSummary("Day wedding event (no overnight accommodation required)");
                StringBuilder sb = new StringBuilder();
                sb.append("Dear Guest,\n\nThank you for considering our estate for your wedding celebration. ");
                sb.append("We have noted that you are planning a day event (with no overnight accommodation required)");
                if (guests != null) {
                    sb.append(" for ").append(guests).append(" guests");
                }
                if (dateHint != null) {
                    sb.append(" for your requested date (").append(dateHint).append(")");
                }
                sb.append(".\n\nOur team will verify venue and facility availability for your requested date and get in touch to prepare a tailored proposal for your celebration.\n\nWarm regards,\nWeddings Team");
                extraction.setSuggestedReply(sb.toString());
            } else if (req == AccommodationRequirement.REQUIRED) {
                extraction.setSummary("Wedding celebration with overnight accommodation");
                StringBuilder sb = new StringBuilder();
                sb.append("Dear Guest,\n\nThank you for considering our estate for your wedding celebration. ");
                sb.append("We have noted your wedding celebration request");
                if (guests != null) {
                    sb.append(" for ").append(guests).append(" guests");
                }
                if (nights != null) {
                    sb.append(" across ").append(nights).append(" night").append(nights > 1 ? "s" : "");
                }
                if (dateHint != null) {
                    sb.append(" in ").append(dateHint);
                }
                sb.append(".\n\nOur weddings and reservations team will review your accommodation requirements, verify venue and facility availability, and confirm room availability and applicable rates from our tariff schedule to prepare a tailored proposal.\n\nWarm regards,\nWeddings Team");
                extraction.setSuggestedReply(sb.toString());
            } else {
                extraction.setSummary("Wedding celebration enquiry");
                StringBuilder sb = new StringBuilder();
                sb.append("Dear Guest,\n\nThank you for considering our estate for your wedding celebration. ");
                sb.append("We have received your enquiry");
                if (guests != null) {
                    sb.append(" for ").append(guests).append(" guests");
                }
                if (dateHint != null) {
                    sb.append(" for ").append(dateHint);
                }
                sb.append(".\n\nOur team will connect with you shortly to verify venue and facility availability, clarify any accommodation or event requirements, and prepare a tailored proposal.\n\nWarm regards,\nWeddings Team");
                extraction.setSuggestedReply(sb.toString());
            }
            return extraction;

        } else if (isOffsite && !isWedding) {
            extraction.setCategory(EnquiryCategory.CORPORATE_OFFSITE.name());
            extraction.setConfidence(new BigDecimal("0.80"));
            AccommodationRequirement req = EventAccommodationDetector.detect(rawMessage);
            extraction.setAccommodationRequirement(req.name());

            Integer guests = extraction.getGuests();
            Integer nights = extraction.getNights();

            if (req == AccommodationRequirement.NOT_REQUIRED) {
                extraction.setSummary("Day corporate offsite (no overnight accommodation required)");
                StringBuilder sb = new StringBuilder();
                sb.append("Dear Guest,\n\nThank you for reaching out regarding your corporate offsite. ");
                sb.append("We have noted that you are planning a day event (with no overnight accommodation required)");
                if (guests != null) {
                    sb.append(" for a team of ").append(guests);
                }
                if (dateHint != null) {
                    sb.append(" in ").append(dateHint);
                }
                sb.append(".\n\nOur team will verify facility availability for your requested date and get in touch to prepare a tailored proposal for your offsite.\n\nWarm regards,\nCorporate Events Team");
                extraction.setSuggestedReply(sb.toString());
            } else if (req == AccommodationRequirement.REQUIRED) {
                extraction.setSummary("Corporate offsite with overnight accommodation");
                StringBuilder sb = new StringBuilder();
                sb.append("Dear Guest,\n\nThank you for reaching out regarding your corporate offsite. ");
                sb.append("We have noted your request for a team");
                if (guests != null) {
                    sb.append(" of ").append(guests);
                }
                if (nights != null) {
                    sb.append(" across ").append(nights).append(" night").append(nights > 1 ? "s" : "");
                }
                if (dateHint != null) {
                    sb.append(" in ").append(dateHint);
                }
                sb.append(".\n\nOur corporate events and reservations team will review your stay requirements, verify facility availability, and confirm room availability and applicable rates from our tariff schedule to prepare a tailored proposal.\n\nWarm regards,\nCorporate Events Team");
                extraction.setSuggestedReply(sb.toString());
            } else {
                extraction.setSummary("Corporate offsite enquiry");
                StringBuilder sb = new StringBuilder();
                sb.append("Dear Guest,\n\nThank you for reaching out regarding your corporate offsite. ");
                sb.append("We have received your enquiry");
                if (guests != null) {
                    sb.append(" for a team of ").append(guests);
                }
                if (dateHint != null) {
                    sb.append(" in ").append(dateHint);
                }
                sb.append(".\n\nOur corporate events specialist will get in touch with you shortly to verify facility availability, understand your team's schedule and offsite requirements, and prepare a tailored proposal.\n\nWarm regards,\nCorporate Events Team");
                extraction.setSuggestedReply(sb.toString());
            }
            return extraction;

        } else if (isRoom && !isWedding && !isOffsite) {
            extraction.setCategory(EnquiryCategory.ROOM_BOOKING.name());
            extraction.setConfidence(new BigDecimal("0.75"));
            extraction.setAccommodationRequirement(AccommodationRequirement.REQUIRED.name());
            extraction.setSummary("Guest enquiry for room booking");

            Integer guests = extraction.getGuests();
            StringBuilder sb = new StringBuilder();
            sb.append("Dear Guest,\n\nThank you for reaching out to us. We have received your room booking request");
            if (guests != null) {
                sb.append(" for ").append(guests).append(" guest").append(guests > 1 ? "s" : "");
            }
            sb.append(". {{RATE_TABLE_QUOTE_BLOCK}}\n\nOur reservations team will get in touch with you shortly to assist with your reservation.\n\nWarm regards,\nReservations Team");
            extraction.setSuggestedReply(sb.toString());
            return extraction;

        } else {
            // Ambiguous (e.g. both wedding & offsite, or zero matches) -> UNSORTED
            extraction.setCategory(EnquiryCategory.UNSORTED.name());
            extraction.setConfidence(new BigDecimal("0.40"));
            extraction.setSummary("Unclassified enquiry requiring manual staff review");
            extraction.setSuggestedReply("Dear Guest,\n\nThank you for reaching out to us. We have received your enquiry and our team will get in touch with you shortly.\n\nWarm regards,\nEstate Team");
            return extraction;
        }
    }
}
