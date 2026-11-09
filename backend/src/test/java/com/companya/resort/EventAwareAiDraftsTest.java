package com.companya.resort;

import com.companya.resort.dto.CreateEnquiryRequest;
import com.companya.resort.dto.GeminiEnquiryExtraction;
import com.companya.resort.dto.PublicEnquiryResponse;
import com.companya.resort.entity.*;
import com.companya.resort.repository.EnquiryDraftRepository;
import com.companya.resort.repository.EnquiryRepository;
import com.companya.resort.repository.UserRepository;
import com.companya.resort.service.*;
import com.companya.resort.util.EventAccommodationDetector;
import com.companya.resort.util.EventAccommodationDetector.AccommodationRequirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventAwareAiDraftsTest {

    @Mock
    private EnquiryRepository enquiryRepository;

    @Mock
    private EnquiryDraftRepository enquiryDraftRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeminiAiService geminiAiService;

    @Mock
    private QuoteInjectorService quoteInjectorService;

    @Mock
    private SimulatedDeliveryAdapter simulatedDeliveryAdapter;

    private RuleFallbackService ruleFallbackService;
    private EnquiryRoutingService enquiryRoutingService;
    private EnquiryService enquiryService;

    private User gmUser;
    private User weddingStaff;
    private User offsiteStaff;
    private User roomStaff;

    @BeforeEach
    void setUp() {
        ruleFallbackService = new RuleFallbackService();
        enquiryRoutingService = new EnquiryRoutingService(userRepository);

        gmUser = new User("gm@estate.test", "hash", "Vikram Rao (GM)", Role.STAFF, SpecialtyLane.GM_ESCALATION, true, true);
        gmUser.setId(1L);

        weddingStaff = new User("wedding@estate.test", "hash", "Karthik Urs", Role.STAFF, SpecialtyLane.WEDDING, false, true);
        weddingStaff.setId(2L);

        offsiteStaff = new User("offsite@estate.test", "hash", "Meera Nair", Role.STAFF, SpecialtyLane.OFFSITE, false, true);
        offsiteStaff.setId(3L);

        roomStaff = new User("room@estate.test", "hash", "Ananya Hegde", Role.STAFF, SpecialtyLane.ROOM, false, true);
        roomStaff.setId(4L);

        lenient().when(userRepository.findFirstByIsGmTrueAndIsActiveTrueOrderByIdAsc()).thenReturn(Optional.of(gmUser));
        lenient().when(userRepository.findFirstBySpecialtyLaneAndIsActiveTrueOrderByIdAsc(SpecialtyLane.WEDDING)).thenReturn(Optional.of(weddingStaff));
        lenient().when(userRepository.findFirstBySpecialtyLaneAndIsActiveTrueOrderByIdAsc(SpecialtyLane.OFFSITE)).thenReturn(Optional.of(offsiteStaff));
        lenient().when(userRepository.findFirstBySpecialtyLaneAndIsActiveTrueOrderByIdAsc(SpecialtyLane.ROOM)).thenReturn(Optional.of(roomStaff));

        // When quoteInjectorService is called, simulate real behavior
        lenient().when(quoteInjectorService.injectDatabaseQuote(any(), any()))
                .thenAnswer(inv -> {
                    String draft = inv.getArgument(0);
                    Enquiry enq = inv.getArgument(1);
                    // Real injector delegation
                    QuoteInjectorService realInjector = new QuoteInjectorService(null, null, null, null, null, null);
                    // Use simple sanitization logic matching real QuoteInjectorService
                    if (enq.getCategory() == EnquiryCategory.ROOM_BOOKING) {
                        return new QuoteInjectorService.InjectedQuote(
                                draft.replace("{{RATE_TABLE_QUOTE_BLOCK}}", "The standard rate for your dates is ₹17,000 + GST."),
                                "{\"status\":\"MOCK\"}"
                        );
                    }
                    if (enq.getCategory() == EnquiryCategory.WEDDING || enq.getCategory() == EnquiryCategory.CORPORATE_OFFSITE) {
                        AccommodationRequirement req = EventAccommodationDetector.detect(enq.getRawMessage());
                        if (req == AccommodationRequirement.REQUIRED) {
                            return new QuoteInjectorService.InjectedQuote(
                                    draft.replace("{{RATE_TABLE_QUOTE_BLOCK}}", "Our team will confirm room availability and applicable rates from our tariff schedule."),
                                    null
                            );
                        } else {
                            return new QuoteInjectorService.InjectedQuote(
                                    draft.replace("{{RATE_TABLE_QUOTE_BLOCK}}", "").trim(),
                                    null
                            );
                        }
                    }
                    return new QuoteInjectorService.InjectedQuote(draft.replace("{{RATE_TABLE_QUOTE_BLOCK}}", "").trim(), null);
                });

        enquiryService = new EnquiryService(
                enquiryRepository,
                enquiryDraftRepository,
                userRepository,
                geminiAiService,
                ruleFallbackService,
                enquiryRoutingService,
                quoteInjectorService,
                simulatedDeliveryAdapter,
                new BigDecimal("0.80")
        );

        lenient().when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(enquiryDraftRepository.save(any(EnquiryDraft.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("1. Wedding day event without overnight accommodation (Manus finding scenario)")
    void testWeddingDayEventWithoutOvernightAccommodation() {
        String message = "Day event only (no overnight accommodation required). Planning our wedding reception for 150 guests on 15 Feb 2027.";

        // Test detector directly
        AccommodationRequirement req = EventAccommodationDetector.detect(message);
        assertEquals(AccommodationRequirement.NOT_REQUIRED, req);

        // Test fallback extraction
        GeminiEnquiryExtraction extraction = ruleFallbackService.classifyFallback(message);
        assertEquals(EnquiryCategory.WEDDING.name(), extraction.getCategory());
        assertEquals("NOT_REQUIRED", extraction.getAccommodationRequirement());

        String draft = extraction.getSuggestedReply();
        assertNotNull(draft);

        // Strict assertions matching Manus finding
        assertFalse(draft.toLowerCase().contains("requested stay"), "Must not refer to 'requested stay'");
        assertFalse(draft.toLowerCase().contains("your stay"), "Must not refer to 'your stay'");
        assertFalse(draft.toLowerCase().contains("room availability"), "Must not refer to 'room availability'");
        assertFalse(draft.toLowerCase().contains("room rates"), "Must not refer to 'room rates'");
        assertFalse(draft.toLowerCase().contains("requested room booking"), "Must not refer to 'requested room booking'");
        assertFalse(draft.contains("{{RATE_TABLE_QUOTE_BLOCK}}"), "Must not contain rate table placeholder");

        assertTrue(draft.toLowerCase().contains("day event"), "Must acknowledge day-event requirement");
        assertTrue(draft.toLowerCase().contains("150 guests"), "Must use supplied guest count");
        assertTrue(draft.toLowerCase().contains("venue availability") || draft.toLowerCase().contains("proposal"), "Must focus on venue availability and proposal");
    }

    @Test
    @DisplayName("2. Wedding with overnight accommodation requested")
    void testWeddingWithOvernightAccommodationRequested() {
        String message = "Venue for my sister's wedding in Feb 2027, about 250 guests, 2 nights stay";

        AccommodationRequirement req = EventAccommodationDetector.detect(message);
        assertEquals(AccommodationRequirement.REQUIRED, req);

        GeminiEnquiryExtraction extraction = ruleFallbackService.classifyFallback(message);
        assertEquals(EnquiryCategory.WEDDING.name(), extraction.getCategory());
        assertEquals("REQUIRED", extraction.getAccommodationRequirement());

        String draft = extraction.getSuggestedReply();
        assertTrue(draft.toLowerCase().contains("wedding celebration"), "Must mention wedding celebration");
        assertTrue(draft.toLowerCase().contains("250 guests"), "Must mention 250 guests");
        assertTrue(draft.toLowerCase().contains("tariff schedule"), "Must state that team will confirm room availability and rates from tariff schedule");
    }

    @Test
    @DisplayName("3. Wedding with accommodation not specified")
    void testWeddingWithAccommodationUnspecified() {
        String message = "Looking for a wedding venue for 200 guests in April 2027, please share details.";

        AccommodationRequirement req = EventAccommodationDetector.detect(message);
        assertEquals(AccommodationRequirement.UNSPECIFIED, req);

        GeminiEnquiryExtraction extraction = ruleFallbackService.classifyFallback(message);
        assertEquals(EnquiryCategory.WEDDING.name(), extraction.getCategory());
        assertEquals("UNSPECIFIED", extraction.getAccommodationRequirement());

        String draft = extraction.getSuggestedReply();
        // Neutral wording: does NOT assume stay, does NOT assume day event
        assertFalse(draft.toLowerCase().contains("requested stay"));
        assertTrue(draft.toLowerCase().contains("wedding celebration"));
        assertTrue(draft.toLowerCase().contains("200 guests"));
        assertTrue(draft.toLowerCase().contains("clarify any accommodation"), "Must offer to clarify accommodation needs without assuming either");
    }

    @Test
    @DisplayName("4. Corporate offsite with accommodation required")
    void testCorporateOffsiteWithAccommodationRequired() {
        String message = "Planning a 2 nights corporate leadership retreat for team of 30 with conference hall and rooms.";

        AccommodationRequirement req = EventAccommodationDetector.detect(message);
        assertEquals(AccommodationRequirement.REQUIRED, req);

        GeminiEnquiryExtraction extraction = ruleFallbackService.classifyFallback(message);
        assertEquals(EnquiryCategory.CORPORATE_OFFSITE.name(), extraction.getCategory());
        assertEquals("REQUIRED", extraction.getAccommodationRequirement());

        String draft = extraction.getSuggestedReply();
        assertTrue(draft.toLowerCase().contains("corporate offsite"));
        assertTrue(draft.toLowerCase().contains("team of 30"));
        assertTrue(draft.toLowerCase().contains("tariff schedule"), "Must mention confirming room availability and rates from tariff schedule");
    }

    @Test
    @DisplayName("5. Corporate offsite without accommodation (day conference)")
    void testCorporateOffsiteWithoutAccommodation() {
        String message = "Day event only (no overnight accommodation required). Conference hall for team of 45 for our annual review meeting.";

        AccommodationRequirement req = EventAccommodationDetector.detect(message);
        assertEquals(AccommodationRequirement.NOT_REQUIRED, req);

        GeminiEnquiryExtraction extraction = ruleFallbackService.classifyFallback(message);
        assertEquals(EnquiryCategory.CORPORATE_OFFSITE.name(), extraction.getCategory());
        assertEquals("NOT_REQUIRED", extraction.getAccommodationRequirement());

        String draft = extraction.getSuggestedReply();
        assertFalse(draft.toLowerCase().contains("requested stay"));
        assertFalse(draft.toLowerCase().contains("room rates"));
        assertFalse(draft.toLowerCase().contains("room availability"));

        assertTrue(draft.toLowerCase().contains("day event"));
        assertTrue(draft.toLowerCase().contains("meeting space") || draft.toLowerCase().contains("day package proposal"));
    }

    @Test
    @DisplayName("6. Corporate offsite with accommodation unspecified (ENQ-4412)")
    void testCorporateOffsiteAccommodationUnspecified() {
        String message = "Team of 45 wants a January offsite with a conference hall";

        AccommodationRequirement req = EventAccommodationDetector.detect(message);
        assertEquals(AccommodationRequirement.UNSPECIFIED, req);

        GeminiEnquiryExtraction extraction = ruleFallbackService.classifyFallback(message);
        assertEquals(EnquiryCategory.CORPORATE_OFFSITE.name(), extraction.getCategory());
        assertEquals("UNSPECIFIED", extraction.getAccommodationRequirement());

        String draft = extraction.getSuggestedReply();
        assertFalse(draft.toLowerCase().contains("requested stay"), "Must not invent stay for unspecified offsite");
        assertTrue(draft.toLowerCase().contains("team of 45"));
        assertTrue(draft.toLowerCase().contains("clarify any accommodation") || draft.toLowerCase().contains("conference space"));
    }

    @Test
    @DisplayName("7. Normal room enquiry (ENQ-4411)")
    void testNormalRoomEnquiry() {
        String message = "Is a villa free 14-16 Nov for 2 adults and a child?";

        GeminiEnquiryExtraction extraction = ruleFallbackService.classifyFallback(message);
        assertEquals(EnquiryCategory.ROOM_BOOKING.name(), extraction.getCategory());
        assertEquals("REQUIRED", extraction.getAccommodationRequirement());

        String draft = extraction.getSuggestedReply();
        assertTrue(draft.toLowerCase().contains("room booking request"));
        assertTrue(draft.contains("{{RATE_TABLE_QUOTE_BLOCK}}"), "Room enquiry must contain rate table quote placeholder");
    }

    @Test
    @DisplayName("8. Low-confidence enquiry routed to GM unsorted")
    void testLowConfidenceEnquiryRoutedToGmUnsorted() {
        String message = "Hi, I have a quick question. Can you please call me back?";

        when(geminiAiService.processEnquiry(anyString()))
                .thenReturn(GeminiAiService.AiProcessingResult.failure("LOW_CONFIDENCE", "Confidence below threshold"));

        CreateEnquiryRequest request = new CreateEnquiryRequest("Guest", "guest@example.test", message);
        PublicEnquiryResponse response = enquiryService.createPublicEnquiry(request);

        assertNotNull(response);
        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository, atLeastOnce()).save(captor.capture());

        Enquiry saved = captor.getValue();
        assertEquals(EnquiryCategory.UNSORTED, saved.getCategory());
        assertEquals(gmUser, saved.getAssignedToUser(), "Unsorted enquiry must be assigned to General Manager");
    }

    @Test
    @DisplayName("9. Complaint escalated to GM")
    void testComplaintEscalatedToGm() {
        String message = "Worst service ever! Power was cut for 4 hours and hot water was not working. Extremely disappointed!";

        when(geminiAiService.processEnquiry(anyString()))
                .thenReturn(GeminiAiService.AiProcessingResult.failure("API_OFFLINE", "Offline fallback"));

        CreateEnquiryRequest request = new CreateEnquiryRequest("Upset Guest", "upset@example.test", message);
        PublicEnquiryResponse response = enquiryService.createPublicEnquiry(request);

        assertNotNull(response);
        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository, atLeastOnce()).save(captor.capture());

        Enquiry saved = captor.getValue();
        assertEquals(EnquiryCategory.COMPLAINT, saved.getCategory());
        assertTrue(saved.getGmAlerted(), "Complaint must flag GM alert");
        assertEquals(gmUser, saved.getAssignedToUser(), "Complaint must be routed directly to GM");

        ArgumentCaptor<EnquiryDraft> draftCaptor = ArgumentCaptor.forClass(EnquiryDraft.class);
        verify(enquiryDraftRepository).save(draftCaptor.capture());
        EnquiryDraft draft = draftCaptor.getValue();
        assertTrue(draft.getDraftBody().contains("sincerely apologize"), "Must generate empathetic apology");
        assertTrue(draft.getDraftBody().contains("General Manager"), "Must mention General Manager follow-up");
        assertFalse(draft.getDraftBody().toLowerCase().contains("requested stay"), "Must not mention stay in complaint");
    }

    @Test
    @DisplayName("10. Deterministic fallback when Gemini is unavailable")
    void testDeterministicFallbackWhenGeminiUnavailable() {
        String message = "Day event only (no overnight accommodation required). Wedding lawn for 250 guests in Feb 2027.";

        // Simulate Gemini offline / missing API key
        when(geminiAiService.processEnquiry(anyString()))
                .thenReturn(GeminiAiService.AiProcessingResult.failure("MISSING_API_KEY", "Gemini API key is not configured"));

        CreateEnquiryRequest request = new CreateEnquiryRequest("Sumanth", "sumanth@example.test", message);
        enquiryService.createPublicEnquiry(request);

        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository, atLeastOnce()).save(captor.capture());

        Enquiry saved = captor.getValue();
        assertEquals(EnquiryCategory.UNSORTED, saved.getCategory(), "AI failure routes to UNSORTED for GM triage");
        assertEquals(gmUser, saved.getAssignedToUser(), "Must route to GM for review");

        ArgumentCaptor<EnquiryDraft> draftCaptor = ArgumentCaptor.forClass(EnquiryDraft.class);
        verify(enquiryDraftRepository).save(draftCaptor.capture());
        EnquiryDraft draft = draftCaptor.getValue();

        // Verify draft body is fully deterministic and event-aware
        assertFalse(draft.getDraftBody().toLowerCase().contains("requested stay"));
        assertFalse(draft.getDraftBody().toLowerCase().contains("room availability"));
        assertTrue(draft.getDraftBody().toLowerCase().contains("day event"));
        assertTrue(draft.getDraftBody().toLowerCase().contains("proposal"));
    }
}
