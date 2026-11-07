package com.companya.resort;

import com.companya.resort.dto.AdminEnquiryDetailDto;
import com.companya.resort.dto.ApproveDraftRequest;
import com.companya.resort.dto.CreateEnquiryRequest;
import com.companya.resort.dto.DraftApprovalResponse;
import com.companya.resort.dto.GeminiEnquiryExtraction;
import com.companya.resort.dto.PublicEnquiryResponse;
import com.companya.resort.dto.RouteEnquiryRequest;
import com.companya.resort.dto.SimulateEnquiryRequest;
import com.companya.resort.entity.DraftStatus;
import com.companya.resort.entity.Enquiry;
import com.companya.resort.entity.EnquiryCategory;
import com.companya.resort.entity.EnquiryChannel;
import com.companya.resort.entity.EnquiryDraft;
import com.companya.resort.entity.EnquiryStatus;
import com.companya.resort.entity.Role;
import com.companya.resort.entity.RoutingMethod;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import com.companya.resort.repository.EnquiryDraftRepository;
import com.companya.resort.repository.EnquiryRepository;
import com.companya.resort.repository.UserRepository;
import com.companya.resort.security.UserPrincipal;
import com.companya.resort.service.EnquiryRoutingService;
import com.companya.resort.service.EnquiryService;
import com.companya.resort.service.GeminiAiService;
import com.companya.resort.service.QuoteInjectorService;
import com.companya.resort.service.RuleFallbackService;
import com.companya.resort.service.SimulatedDeliveryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EnquiryServiceTest {

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
    private User roomStaff;
    private User offsiteStaff;

    private UserPrincipal gmPrincipal;
    private UserPrincipal roomPrincipal;
    private UserPrincipal weddingPrincipal;
    private UserPrincipal offsitePrincipal;

    @BeforeEach
    void setUp() {
        ruleFallbackService = new RuleFallbackService();
        enquiryRoutingService = new EnquiryRoutingService(userRepository);

        gmUser = new User("gm@estate.com", "hash", "Vikram Rao", Role.STAFF, SpecialtyLane.GM_ESCALATION, true);
        gmUser.setId(1L);

        roomStaff = new User("rooms@estate.com", "hash", "Ananya Hegde", Role.STAFF, SpecialtyLane.ROOM, false);
        roomStaff.setId(2L);

        weddingStaff = new User("wedding@estate.com", "hash", "Karthik Urs", Role.STAFF, SpecialtyLane.WEDDING, false);
        weddingStaff.setId(3L);

        offsiteStaff = new User("offsite@estate.com", "hash", "Meera Nair", Role.STAFF, SpecialtyLane.OFFSITE, false);
        offsiteStaff.setId(4L);

        gmPrincipal = new UserPrincipal(gmUser.getId(), gmUser.getEmail(), gmUser.getRole().name(), gmUser.getIsGm(), gmUser.getSpecialtyLane().name());
        roomPrincipal = new UserPrincipal(roomStaff.getId(), roomStaff.getEmail(), roomStaff.getRole().name(), roomStaff.getIsGm(), roomStaff.getSpecialtyLane().name());
        weddingPrincipal = new UserPrincipal(weddingStaff.getId(), weddingStaff.getEmail(), weddingStaff.getRole().name(), weddingStaff.getIsGm(), weddingStaff.getSpecialtyLane().name());
        offsitePrincipal = new UserPrincipal(offsiteStaff.getId(), offsiteStaff.getEmail(), offsiteStaff.getRole().name(), offsiteStaff.getIsGm(), offsiteStaff.getSpecialtyLane().name());

        when(userRepository.findFirstByIsGmTrueOrderByIdAsc()).thenReturn(Optional.of(gmUser));
        when(userRepository.findFirstBySpecialtyLaneOrderByIdAsc(SpecialtyLane.ROOM)).thenReturn(Optional.of(roomStaff));
        when(userRepository.findFirstBySpecialtyLaneOrderByIdAsc(SpecialtyLane.WEDDING)).thenReturn(Optional.of(weddingStaff));
        when(userRepository.findFirstBySpecialtyLaneOrderByIdAsc(SpecialtyLane.OFFSITE)).thenReturn(Optional.of(offsiteStaff));
        when(userRepository.findFirstBySpecialtyLaneOrderByIdAsc(SpecialtyLane.GM_ESCALATION)).thenReturn(Optional.of(gmUser));

        when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(invocation -> {
            Enquiry e = invocation.getArgument(0);
            if (e.getId() == null) {
                e = spy(e);
                doReturn(101L).when(e).getId();
            }
            return e;
        });

        when(enquiryDraftRepository.save(any(EnquiryDraft.class))).thenAnswer(invocation -> {
            EnquiryDraft d = invocation.getArgument(0);
            if (d.getId() == null) {
                d = spy(d);
                doReturn(201L).when(d).getId();
            }
            return d;
        });

        when(enquiryDraftRepository.atomicClaimDraft(any(), any(), any()))
                .thenReturn(1);
        when(enquiryDraftRepository.atomicDiscardDraft(any(), any(), any()))
                .thenReturn(1);

        when(quoteInjectorService.injectDatabaseQuote(anyString(), any(Enquiry.class)))
                .thenAnswer(invocation -> new QuoteInjectorService.InjectedQuote(invocation.getArgument(0), null));

        when(quoteInjectorService.injectDatabaseQuote(anyString(), any(Enquiry.class)))
                .thenAnswer(inv -> new QuoteInjectorService.InjectedQuote(inv.getArgument(0), null));

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
    }

    @Test
    @DisplayName("ENQ-4410: Wedding enquiry is categorized as WEDDING and routed to Wedding staff")
    void testEnquiry4410_WeddingExtractionAndRouting() {
        String rawMessage = "Venue for my sister's wedding in Feb 2027, about 250 guests, 2 nights";

        GeminiEnquiryExtraction extraction = new GeminiEnquiryExtraction();
        extraction.setCategory("WEDDING");
        extraction.setConfidence(new BigDecimal("0.95"));
        extraction.setSummary("Wedding enquiry for 250 guests in Feb 2027");
        extraction.setGuests(250);
        extraction.setNights(2);
        extraction.setCheckInDate("2027-02-12");
        extraction.setCheckOutDate("2027-02-14");
        extraction.setSuggestedReply("Dear Guest, Thank you for considering our estate for your sister's wedding.");

        when(geminiAiService.processEnquiry(rawMessage))
                .thenReturn(GeminiAiService.AiProcessingResult.success(extraction));

        CreateEnquiryRequest request = new CreateEnquiryRequest("Rahul Sharma", "rahul@example.com", rawMessage);
        PublicEnquiryResponse response = enquiryService.createPublicEnquiry(request);

        assertNotNull(response);
        assertNotNull(response.getEnquiryCode());
        assertEquals("OPEN", response.getStatus());

        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository, atLeastOnce()).save(captor.capture());
        Enquiry saved = captor.getValue();

        assertEquals(EnquiryCategory.WEDDING, saved.getCategory());
        assertEquals(RoutingMethod.AI_HIGH_CONFIDENCE, saved.getRoutingMethod());
        assertEquals(weddingStaff, saved.getAssignedToUser());
        assertEquals(250, saved.getExtractedGuests());
        assertEquals(2, saved.getExtractedNights());
        assertEquals(LocalDate.of(2027, 2, 12), saved.getExtractedCheckIn());
        assertEquals(LocalDate.of(2027, 2, 14), saved.getExtractedCheckOut());
        assertFalse(saved.getGmAlerted());
    }

    @Test
    @DisplayName("ENQ-4411: Room booking enquiry is categorized as ROOM_BOOKING and routed to Room staff")
    void testEnquiry4411_RoomBookingExtractionAndRouting() {
        String rawMessage = "Is a villa free 14-16 Nov for 2 adults and a child?";

        GeminiEnquiryExtraction extraction = new GeminiEnquiryExtraction();
        extraction.setCategory("ROOM_BOOKING");
        extraction.setConfidence(new BigDecimal("0.92"));
        extraction.setSummary("Villa booking query for 14-16 Nov for 3 guests");
        extraction.setGuests(3);
        extraction.setRooms(1);
        extraction.setCheckInDate("2026-11-14");
        extraction.setCheckOutDate("2026-11-16");
        extraction.setNights(2);
        extraction.setSuggestedReply("Dear Guest, {{RATE_TABLE_QUOTE_BLOCK}}\nWarm regards.");

        when(geminiAiService.processEnquiry(rawMessage))
                .thenReturn(GeminiAiService.AiProcessingResult.success(extraction));

        when(quoteInjectorService.injectDatabaseQuote(anyString(), any(Enquiry.class)))
                .thenReturn(new QuoteInjectorService.InjectedQuote("The rate is ₹14,000 per night", "{\"rate\": 14000}"));

        CreateEnquiryRequest request = new CreateEnquiryRequest("Deepak Verma", "+919876543210", rawMessage);
        PublicEnquiryResponse response = enquiryService.createPublicEnquiry(request);

        assertNotNull(response);
        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository, atLeastOnce()).save(captor.capture());
        Enquiry saved = captor.getValue();

        assertEquals(EnquiryCategory.ROOM_BOOKING, saved.getCategory());
        assertEquals(RoutingMethod.AI_HIGH_CONFIDENCE, saved.getRoutingMethod());
        assertEquals(roomStaff, saved.getAssignedToUser());

        ArgumentCaptor<EnquiryDraft> draftCaptor = ArgumentCaptor.forClass(EnquiryDraft.class);
        verify(enquiryDraftRepository).save(draftCaptor.capture());
        EnquiryDraft draft = draftCaptor.getValue();
        assertEquals(DraftStatus.DRAFT_PENDING_REVIEW, draft.getStatus());
        assertTrue(draft.getDraftBody().contains("₹14,000"));
    }

    @Test
    @DisplayName("ENQ-4412: Corporate offsite enquiry is categorized as CORPORATE_OFFSITE and routed to Offsites staff")
    void testEnquiry4412_CorporateOffsiteExtractionAndRouting() {
        String rawMessage = "Team of 45 wants a January offsite with a conference hall.";

        GeminiEnquiryExtraction extraction = new GeminiEnquiryExtraction();
        extraction.setCategory("CORPORATE_OFFSITE");
        extraction.setConfidence(new BigDecimal("0.89"));
        extraction.setSummary("Corporate offsite for 45 pax with conference hall in January");
        extraction.setGuests(45);
        extraction.setSuggestedReply("Dear Team, We would be delighted to host your corporate offsite.");

        when(geminiAiService.processEnquiry(rawMessage))
                .thenReturn(GeminiAiService.AiProcessingResult.success(extraction));

        SimulateEnquiryRequest request = new SimulateEnquiryRequest(
                EnquiryChannel.EMAIL_SIM, "Infosys Team", "events@infosys.com", rawMessage
        );

        when(enquiryRepository.findById(101L)).thenAnswer(inv -> {
            Enquiry e = new Enquiry();
            e.setId(101L);
            e.setCategory(EnquiryCategory.CORPORATE_OFFSITE);
            e.setAssignedToUser(offsiteStaff);
            return Optional.of(e);
        });

        AdminEnquiryDetailDto response = enquiryService.simulateEnquiry(request);
        assertNotNull(response);
        assertEquals("CORPORATE_OFFSITE", response.getCategory());
        assertEquals("Meera Nair", response.getAssignedToUserName());
        verify(simulatedDeliveryAdapter, never()).deliver(any(), any());
    }

    @Test
    @DisplayName("Simulate enquiry with WHATSAPP_SIM preserves channel, creates pending review draft, and does not auto-deliver")
    void testSimulateEnquiry_WhatsAppSim_PreservesChannelAndDoesNotAutoDeliver() {
        String rawMessage = "Need 3 rooms for family getaway from Dec 10 to Dec 13. How much will it cost?";

        GeminiEnquiryExtraction extraction = new GeminiEnquiryExtraction();
        extraction.setCategory("ROOM_BOOKING");
        extraction.setConfidence(new BigDecimal("0.95"));
        extraction.setSummary("Room booking enquiry for family getaway");
        extraction.setSuggestedReply("Hello, thank you for contacting Chikkamagaluru Estate.");

        when(geminiAiService.processEnquiry(rawMessage))
                .thenReturn(GeminiAiService.AiProcessingResult.success(extraction));

        SimulateEnquiryRequest request = new SimulateEnquiryRequest(
                EnquiryChannel.WHATSAPP_SIM, "Suresh Kumar", "+919876543210", rawMessage
        );

        when(enquiryRepository.findById(102L)).thenAnswer(inv -> {
            Enquiry e = new Enquiry();
            e.setId(102L);
            e.setSourceChannel(EnquiryChannel.WHATSAPP_SIM);
            e.setCategory(EnquiryCategory.ROOM_BOOKING);
            e.setAssignedToUser(roomStaff);
            return Optional.of(e);
        });

        AdminEnquiryDetailDto response = enquiryService.simulateEnquiry(request);
        assertNotNull(response);
        assertEquals("WHATSAPP_SIM", response.getSourceChannel());
        assertEquals("ROOM_BOOKING", response.getCategory());
        assertEquals("Ananya Hegde", response.getAssignedToUserName());

        // Verify draft was created in pending review status
        verify(enquiryDraftRepository, atLeastOnce()).save(any(EnquiryDraft.class));

        // CRITICAL: verify message was NEVER auto-delivered upon simulation
        verify(simulatedDeliveryAdapter, never()).deliver(any(), any());
    }

    @Test
    @DisplayName("Simulate enquiry rejects unsupported channels with IllegalArgumentException")
    void testSimulateEnquiry_UnsupportedChannel_ThrowsIllegalArgumentException() {
        SimulateEnquiryRequest request = new SimulateEnquiryRequest(
                EnquiryChannel.WEB_FORM, "Test User", "test@example.com", "Test web message"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                enquiryService.simulateEnquiry(request)
        );
        assertTrue(ex.getMessage().contains("Unsupported simulation channel"));
    }

    @Test
    @DisplayName("Complaint enquiry triggers GM alert and routes to General Manager")
    void testComplaint_TriggersGmAlertAndRoutesToGm() {
        String rawMessage = "Terrible experience, water was dirty and reception staff was extremely rude!";

        GeminiEnquiryExtraction extraction = new GeminiEnquiryExtraction();
        extraction.setCategory("COMPLAINT");
        extraction.setConfidence(new BigDecimal("0.96"));
        extraction.setSummary("Guest complaint regarding dirty water and rude staff");
        extraction.setSuggestedReply("We apologize sincerely for your experience.");

        when(geminiAiService.processEnquiry(rawMessage))
                .thenReturn(GeminiAiService.AiProcessingResult.success(extraction));

        CreateEnquiryRequest request = new CreateEnquiryRequest("Angry Guest", "guest@complaint.com", rawMessage);
        enquiryService.createPublicEnquiry(request);

        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository, atLeastOnce()).save(captor.capture());
        Enquiry saved = captor.getValue();

        assertEquals(EnquiryCategory.COMPLAINT, saved.getCategory());
        assertTrue(saved.getGmAlerted(), "GM Alert must be TRUE for complaints");
        assertEquals(gmUser, saved.getAssignedToUser(), "Complaints must route to GM");
    }

    @Test
    @DisplayName("Low confidence classification (< 0.80) is routed to UNSORTED and assigned to GM")
    void testLowConfidence_RoutedToUnsortedAndGm() {
        String rawMessage = "Can I bring my pet dog to your property?";

        GeminiEnquiryExtraction extraction = new GeminiEnquiryExtraction();
        extraction.setCategory("ROOM_BOOKING");
        extraction.setConfidence(new BigDecimal("0.65")); // Below 0.80 threshold
        extraction.setSummary("Pet policy inquiry");
        extraction.setSuggestedReply("Dear Guest, We welcome pets in specific cottage units.");

        when(geminiAiService.processEnquiry(rawMessage))
                .thenReturn(GeminiAiService.AiProcessingResult.success(extraction));

        CreateEnquiryRequest request = new CreateEnquiryRequest("Pet Owner", "owner@example.com", rawMessage);
        enquiryService.createPublicEnquiry(request);

        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository, atLeastOnce()).save(captor.capture());
        Enquiry saved = captor.getValue();

        assertEquals(EnquiryCategory.UNSORTED, saved.getCategory(), "Low confidence should be forced to UNSORTED");
        assertEquals(RoutingMethod.AI_LOW_CONFIDENCE_GM, saved.getRoutingMethod());
        assertEquals(gmUser, saved.getAssignedToUser(), "Low confidence unsorted must be in GM queue");
    }

    @Test
    @DisplayName("Unknown dates or budgets remain null and are never invented")
    void testMissingDatesOrBudgets_RemainNull() {
        String rawMessage = "Do you have cottages available for a small family?";

        GeminiEnquiryExtraction extraction = new GeminiEnquiryExtraction();
        extraction.setCategory("ROOM_BOOKING");
        extraction.setConfidence(new BigDecimal("0.85"));
        extraction.setSummary("Cottage availability query");
        extraction.setCheckInDate(null);
        extraction.setCheckOutDate(null);
        extraction.setBudgetAmount(null);
        extraction.setSuggestedReply("Dear Guest, Thank you for contacting us.");

        when(geminiAiService.processEnquiry(rawMessage))
                .thenReturn(GeminiAiService.AiProcessingResult.success(extraction));

        CreateEnquiryRequest request = new CreateEnquiryRequest("Guest", "guest@example.com", rawMessage);
        enquiryService.createPublicEnquiry(request);

        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository, atLeastOnce()).save(captor.capture());
        Enquiry saved = captor.getValue();

        assertNull(saved.getExtractedCheckIn(), "Missing check-in date must remain null");
        assertNull(saved.getExtractedCheckOut(), "Missing check-out date must remain null");
        assertNull(saved.getExtractedBudgetInr(), "Missing budget must remain null");
    }

    @Test
    @DisplayName("AI failure with confirmed complaint detects COMPLAINT via rule fallback, alerts GM, and routes to GM")
    void testAiFailure_WithComplaintMessage_ClassifiedAsComplaintAndAlertsGm() {
        String complaintMessage = "I am deeply disappointed with the hot water failure during our stay in villa 3 yesterday. This is completely unacceptable for a luxury property and ruined our anniversary.";

        when(geminiAiService.processEnquiry(complaintMessage))
                .thenReturn(GeminiAiService.AiProcessingResult.failure("API_ERROR", "Gemini 400 Bad Request"));

        CreateEnquiryRequest request = new CreateEnquiryRequest("Rajesh Gupte", "rajesh.gupte@example.com", complaintMessage);
        enquiryService.createPublicEnquiry(request);

        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository, atLeastOnce()).save(captor.capture());
        Enquiry saved = captor.getValue();

        assertEquals(EnquiryCategory.COMPLAINT, saved.getCategory(), "Complaint fallback must classify as COMPLAINT");
        assertTrue(saved.getGmAlerted(), "gm_alerted must be TRUE for complaint fallback");
        assertEquals(gmUser, saved.getAssignedToUser(), "Complaints must route to GM");
        assertEquals("API_ERROR", saved.getAiErrorStatus(), "Error status must be preserved");
        assertEquals(RoutingMethod.RULE_FALLBACK, saved.getRoutingMethod());

        ArgumentCaptor<EnquiryDraft> draftCaptor = ArgumentCaptor.forClass(EnquiryDraft.class);
        verify(enquiryDraftRepository, atLeastOnce()).save(draftCaptor.capture());
        EnquiryDraft draft = draftCaptor.getValue();
        assertTrue(draft.getDraftBody().contains("apologize"), "Draft should contain complaint apology template");
    }

    @Test
    @DisplayName("AI failure with ordinary policy inquiry does not trigger false complaint, remaining in UNSORTED without GM alert")
    void testAiFailure_WithOrdinaryInquiry_KeepsUnsortedWithoutGmAlert() {
        String policyInquiry = "Can you share your refund policy and cancellation terms for monsoon bookings?";

        when(geminiAiService.processEnquiry(policyInquiry))
                .thenReturn(GeminiAiService.AiProcessingResult.failure("TIMEOUT", "Request timed out"));

        CreateEnquiryRequest request = new CreateEnquiryRequest("Inquiring Guest", "guest@example.com", policyInquiry);
        enquiryService.createPublicEnquiry(request);

        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository, atLeastOnce()).save(captor.capture());
        Enquiry saved = captor.getValue();

        assertEquals(EnquiryCategory.UNSORTED, saved.getCategory(), "Policy inquiries on AI error should remain in UNSORTED");
        assertFalse(saved.getGmAlerted(), "Ordinary policy question must NOT trigger GM alert");
        assertEquals(gmUser, saved.getAssignedToUser(), "Unsorted queue is assigned to GM for review");
    }

    @Test
    @DisplayName("AI timeout preserves original enquiry in UNSORTED and records error status")
    void testAiTimeout_PreservesEnquiryAndSetsUnsorted() {
        String rawMessage = "Wedding query for next year";

        when(geminiAiService.processEnquiry(rawMessage))
                .thenReturn(GeminiAiService.AiProcessingResult.failure("TIMEOUT", "Gemini API request timed out"));

        CreateEnquiryRequest request = new CreateEnquiryRequest("Bride", "bride@example.com", rawMessage);
        PublicEnquiryResponse response = enquiryService.createPublicEnquiry(request);

        assertNotNull(response);
        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository, atLeastOnce()).save(captor.capture());
        Enquiry saved = captor.getValue();

        assertEquals(EnquiryCategory.UNSORTED, saved.getCategory());
        assertEquals(RoutingMethod.RULE_FALLBACK, saved.getRoutingMethod());
        assertEquals("TIMEOUT", saved.getAiErrorStatus());
        assertEquals(gmUser, saved.getAssignedToUser());
        assertEquals("Bride", saved.getSenderName());
        assertEquals(rawMessage, saved.getRawMessage());
    }

    @Test
    @DisplayName("Invalid JSON response from AI preserves enquiry as UNSORTED with error status")
    void testInvalidJson_PreservesEnquiryAndSetsUnsorted() {
        String rawMessage = "Corporate conference enquiry";

        when(geminiAiService.processEnquiry(rawMessage))
                .thenReturn(GeminiAiService.AiProcessingResult.failure("MALFORMED_JSON", "Unexpected end-of-input"));

        CreateEnquiryRequest request = new CreateEnquiryRequest("Corp Lead", "corp@lead.com", rawMessage);
        enquiryService.createPublicEnquiry(request);

        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository, atLeastOnce()).save(captor.capture());
        Enquiry saved = captor.getValue();

        assertEquals(EnquiryCategory.UNSORTED, saved.getCategory());
        assertEquals("MALFORMED_JSON", saved.getAiErrorStatus());
    }

    @Test
    @DisplayName("Missing Gemini API key triggers graceful fallback")
    void testMissingApiKey_GracefulFallback() {
        String rawMessage = "Inquiry about weekend stay";

        when(geminiAiService.processEnquiry(rawMessage))
                .thenReturn(GeminiAiService.AiProcessingResult.failure("MISSING_API_KEY", "Gemini API key is not configured"));

        CreateEnquiryRequest request = new CreateEnquiryRequest("Guest", "guest@mail.com", rawMessage);
        enquiryService.createPublicEnquiry(request);

        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository, atLeastOnce()).save(captor.capture());
        Enquiry saved = captor.getValue();

        assertEquals(EnquiryCategory.UNSORTED, saved.getCategory());
        assertEquals("MISSING_API_KEY", saved.getAiErrorStatus());
        assertEquals(gmUser, saved.getAssignedToUser());
    }

    @Test
    @DisplayName("Authorized GM can reroute an unsorted enquiry to wedding specialist")
    void testStaffRerouteEnquiry() {
        Enquiry existing = new Enquiry();
        existing.setId(55L);
        existing.setCategory(EnquiryCategory.UNSORTED);
        existing.setRoutingMethod(RoutingMethod.RULE_FALLBACK);
        existing.setAssignedToUser(gmUser);

        when(enquiryRepository.findById(55L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(3L)).thenReturn(Optional.of(weddingStaff));

        RouteEnquiryRequest routeReq = new RouteEnquiryRequest(EnquiryCategory.WEDDING, 3L);
        enquiryService.routeEnquiry(55L, gmPrincipal, routeReq);

        assertEquals(EnquiryCategory.WEDDING, existing.getCategory());
        assertEquals(weddingStaff, existing.getAssignedToUser());
        assertEquals(RoutingMethod.MANUAL_OVERRIDE, existing.getRoutingMethod());
    }

    @Test
    @DisplayName("Draft approval is strictly required before simulated delivery, and delivery occurs on approval")
    void testDraftApprovalRequiredBeforeSimulatedDelivery() {
        Enquiry existing = new Enquiry();
        existing.setId(77L);
        existing.setEnquiryCode("ENQ-TEST");
        existing.setSourceChannel(EnquiryChannel.EMAIL_SIM);
        existing.setSenderContact("guest@example.com");
        existing.setStatus(EnquiryStatus.OPEN);
        existing.setCategory(EnquiryCategory.ROOM_BOOKING);
        existing.setAssignedToUser(roomStaff);

        EnquiryDraft draft = new EnquiryDraft();
        draft.setId(88L);
        draft.setEnquiry(existing);
        draft.setDraftBody("Initial AI draft");
        draft.setStatus(DraftStatus.DRAFT_PENDING_REVIEW);

        when(enquiryRepository.findById(77L)).thenReturn(Optional.of(existing));
        when(enquiryDraftRepository.findFirstByEnquiryIdAndStatusOrderByCreatedAtDesc(77L, DraftStatus.DRAFT_PENDING_REVIEW))
                .thenReturn(Optional.of(draft));
        when(userRepository.findById(2L)).thenReturn(Optional.of(roomStaff));
        when(simulatedDeliveryAdapter.deliver(any(), any()))
                .thenReturn(new SimulatedDeliveryAdapter.DeliveryResult("SIM-123", "EMAIL_SIM", "guest@example.com", "DELIVERED_SIMULATED", LocalDateTime.now(), "Preview"));

        // Delivery adapter MUST NOT have been called yet
        verify(simulatedDeliveryAdapter, never()).deliver(any(), any());

        ApproveDraftRequest approveReq = new ApproveDraftRequest("Approved customized message body");
        DraftApprovalResponse response = enquiryService.approveDraft(77L, 2L, approveReq);

        assertNotNull(response);
        assertEquals("REPLIED", response.getEnquiryStatus());
        assertEquals("SENT_MANUALLY", response.getDraftStatus());
        assertEquals("Approved customized message body", draft.getDraftBody());
        assertEquals(roomStaff, draft.getSentByUser());
        assertNotNull(draft.getSentAt());
        assertEquals(EnquiryStatus.REPLIED, existing.getStatus());

        // Delivery adapter is called exactly once upon explicit staff approval
        verify(simulatedDeliveryAdapter, times(1)).deliver(eq(existing), eq(draft));
    }

    @Test
    @DisplayName("No automatic message sending happens during public creation or staff simulation")
    void testNoAutomaticMessageSending() {
        String rawMessage = "Need 3 rooms for family gathering";

        GeminiEnquiryExtraction extraction = new GeminiEnquiryExtraction();
        extraction.setCategory("ROOM_BOOKING");
        extraction.setConfidence(new BigDecimal("0.90"));
        extraction.setSummary("3 rooms enquiry");
        extraction.setSuggestedReply("Draft reply");

        when(geminiAiService.processEnquiry(rawMessage))
                .thenReturn(GeminiAiService.AiProcessingResult.success(extraction));

        enquiryService.createPublicEnquiry(new CreateEnquiryRequest("Guest", "test@guest.com", rawMessage));

        // Ensure delivery adapter is NEVER called during creation
        verify(simulatedDeliveryAdapter, never()).deliver(any(), any());
    }

    @Test
    @DisplayName("Ordinary staff cannot access enquiries outside their assigned specialty lane")
    void testOrdinaryStaffAccessingAnotherLane_ThrowsAccessDenied() {
        Enquiry weddingEnquiry = new Enquiry();
        weddingEnquiry.setId(101L);
        weddingEnquiry.setCategory(EnquiryCategory.WEDDING);
        weddingEnquiry.setAssignedToUser(weddingStaff);

        when(enquiryRepository.findById(101L)).thenReturn(Optional.of(weddingEnquiry));

        // Room staff attempts to get enquiry details for wedding enquiry
        assertThrows(AccessDeniedException.class, () ->
                enquiryService.getEnquiryDetails(101L, roomPrincipal));

        // Room staff attempts to route wedding enquiry
        RouteEnquiryRequest routeReq = new RouteEnquiryRequest(EnquiryCategory.ROOM_BOOKING, roomStaff.getId());
        assertThrows(AccessDeniedException.class, () ->
                enquiryService.routeEnquiry(101L, roomPrincipal, routeReq));
    }

    @Test
    @DisplayName("Ordinary staff cannot access or modify GM-only complaint records")
    void testOrdinaryStaffModifyingComplaint_ThrowsAccessDenied() {
        Enquiry complaintEnquiry = new Enquiry();
        complaintEnquiry.setId(202L);
        complaintEnquiry.setCategory(EnquiryCategory.COMPLAINT);
        complaintEnquiry.setAssignedToUser(gmUser);

        when(enquiryRepository.findById(202L)).thenReturn(Optional.of(complaintEnquiry));

        // Ordinary room staff cannot get details of GM complaint
        assertThrows(AccessDeniedException.class, () ->
                enquiryService.getEnquiryDetails(202L, roomPrincipal));

        // Ordinary room staff cannot route GM complaint
        RouteEnquiryRequest routeReq = new RouteEnquiryRequest(EnquiryCategory.COMPLAINT, gmUser.getId());
        assertThrows(AccessDeniedException.class, () ->
                enquiryService.routeEnquiry(202L, roomPrincipal, routeReq));

        // Ordinary room staff cannot approve draft on GM complaint
        ApproveDraftRequest approveReq = new ApproveDraftRequest("Apology from rooms");
        assertThrows(AccessDeniedException.class, () ->
                enquiryService.approveDraft(202L, roomPrincipal, approveReq));
    }

    @Test
    @DisplayName("Routing validates assignee specialty lane against enquiry category")
    void testUnauthorizedAssigneeSelection_ThrowsAccessDenied() {
        Enquiry roomEnquiry = new Enquiry();
        roomEnquiry.setId(303L);
        roomEnquiry.setCategory(EnquiryCategory.ROOM_BOOKING);
        roomEnquiry.setAssignedToUser(roomStaff);

        when(enquiryRepository.findById(303L)).thenReturn(Optional.of(roomEnquiry));
        when(userRepository.findById(weddingStaff.getId())).thenReturn(Optional.of(weddingStaff));

        // Attempting to route a ROOM_BOOKING enquiry to wedding staff must be rejected
        RouteEnquiryRequest invalidAssigneeReq = new RouteEnquiryRequest(EnquiryCategory.ROOM_BOOKING, weddingStaff.getId());
        assertThrows(AccessDeniedException.class, () ->
                enquiryService.routeEnquiry(303L, gmPrincipal, invalidAssigneeReq));
    }

    @Test
    @DisplayName("Authorized GM accounts can access and route GM complaints")
    void testAuthorizedGmComplaintAccess_Succeeds() {
        Enquiry complaintEnquiry = new Enquiry();
        complaintEnquiry.setId(404L);
        complaintEnquiry.setCategory(EnquiryCategory.COMPLAINT);
        complaintEnquiry.setAssignedToUser(gmUser);
        complaintEnquiry.setStatus(EnquiryStatus.OPEN);
        complaintEnquiry.setEnquiryCode("ENQ-COMPLAINT-1");

        when(enquiryRepository.findById(404L)).thenReturn(Optional.of(complaintEnquiry));
        when(userRepository.findById(gmUser.getId())).thenReturn(Optional.of(gmUser));

        AdminEnquiryDetailDto details = enquiryService.getEnquiryDetails(404L, gmPrincipal);
        assertNotNull(details);
        assertEquals("ENQ-COMPLAINT-1", details.getEnquiryCode());
        assertEquals("COMPLAINT", details.getCategory());

        RouteEnquiryRequest routeReq = new RouteEnquiryRequest(EnquiryCategory.COMPLAINT, gmUser.getId());
        AdminEnquiryDetailDto routed = enquiryService.routeEnquiry(404L, gmPrincipal, routeReq);
        assertNotNull(routed);
        assertEquals("COMPLAINT", routed.getCategory());
    }

    @Test
    @DisplayName("Safe approval of a pending draft succeeds, delivers simulation and transitions status")
    void testApproveDraft_PendingDraftSucceeds() {
        Enquiry roomEnquiry = new Enquiry();
        roomEnquiry.setId(505L);
        roomEnquiry.setEnquiryCode("ENQ-505");
        roomEnquiry.setCategory(EnquiryCategory.ROOM_BOOKING);
        roomEnquiry.setAssignedToUser(roomStaff);
        roomEnquiry.setStatus(EnquiryStatus.OPEN);
        roomEnquiry.setSourceChannel(EnquiryChannel.EMAIL_SIM);
        roomEnquiry.setSenderContact("guest505@example.com");

        EnquiryDraft pendingDraft = new EnquiryDraft();
        pendingDraft.setId(606L);
        pendingDraft.setEnquiry(roomEnquiry);
        pendingDraft.setDraftBody("Pending room quote draft");
        pendingDraft.setStatus(DraftStatus.DRAFT_PENDING_REVIEW);

        when(enquiryRepository.findById(505L)).thenReturn(Optional.of(roomEnquiry));
        when(enquiryDraftRepository.findFirstByEnquiryIdAndStatusOrderByCreatedAtDesc(505L, DraftStatus.DRAFT_PENDING_REVIEW))
                .thenReturn(Optional.of(pendingDraft));
        when(userRepository.findById(roomStaff.getId())).thenReturn(Optional.of(roomStaff));
        when(simulatedDeliveryAdapter.deliver(any(), any()))
                .thenReturn(new SimulatedDeliveryAdapter.DeliveryResult("SIM-505", "EMAIL_SIM", "guest505@example.com", "DELIVERED_SIMULATED", LocalDateTime.now(), "Preview"));

        ApproveDraftRequest approveReq = new ApproveDraftRequest("Approved room quote draft");
        DraftApprovalResponse resp = enquiryService.approveDraft(505L, roomPrincipal, approveReq);

        assertNotNull(resp);
        assertEquals("REPLIED", resp.getEnquiryStatus());
        assertEquals("SENT_MANUALLY", resp.getDraftStatus());
        assertEquals(DraftStatus.SENT_MANUALLY, pendingDraft.getStatus());
        assertEquals(EnquiryStatus.REPLIED, roomEnquiry.getStatus());
        verify(simulatedDeliveryAdapter, times(1)).deliver(eq(roomEnquiry), eq(pendingDraft));
    }

    @Test
    @DisplayName("Repeated approval requests for already-approved drafts are rejected and do not re-dispatch")
    void testRepeatedApprovalRequest_ThrowsIllegalStateException() {
        Enquiry roomEnquiry = new Enquiry();
        roomEnquiry.setId(506L);
        roomEnquiry.setCategory(EnquiryCategory.ROOM_BOOKING);
        roomEnquiry.setAssignedToUser(roomStaff);
        roomEnquiry.setStatus(EnquiryStatus.REPLIED);

        // When draft has already been approved, finding pending draft returns empty
        when(enquiryRepository.findById(506L)).thenReturn(Optional.of(roomEnquiry));
        when(enquiryDraftRepository.findFirstByEnquiryIdAndStatusOrderByCreatedAtDesc(506L, DraftStatus.DRAFT_PENDING_REVIEW))
                .thenReturn(Optional.empty());

        ApproveDraftRequest approveReq = new ApproveDraftRequest("Duplicate approval attempt");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                enquiryService.approveDraft(506L, roomPrincipal, approveReq));

        assertTrue(ex.getMessage().contains("No pending draft"));
        verify(simulatedDeliveryAdapter, never()).deliver(any(), any());
    }

    @Test
    @DisplayName("Rejection of approval when draft is not in pending review state")
    void testRejectionOfApprovalForNonPendingDraft_ThrowsIllegalStateException() {
        Enquiry weddingEnquiry = new Enquiry();
        weddingEnquiry.setId(507L);
        weddingEnquiry.setCategory(EnquiryCategory.WEDDING);
        weddingEnquiry.setAssignedToUser(weddingStaff);

        when(enquiryRepository.findById(507L)).thenReturn(Optional.of(weddingEnquiry));
        when(enquiryDraftRepository.findFirstByEnquiryIdAndStatusOrderByCreatedAtDesc(507L, DraftStatus.DRAFT_PENDING_REVIEW))
                .thenReturn(Optional.empty());

        ApproveDraftRequest approveReq = new ApproveDraftRequest("Attempting approval on missing draft");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                enquiryService.approveDraft(507L, weddingPrincipal, approveReq));

        assertTrue(ex.getMessage().contains("No pending draft"));
        verify(simulatedDeliveryAdapter, never()).deliver(any(), any());
    }

    @Test
    @DisplayName("Authorization failures leave database records unchanged without persistence")
    void testAuthorizationFailureLeavesDatabaseRecordsUnchanged() {
        Enquiry weddingEnquiry = new Enquiry();
        weddingEnquiry.setId(508L);
        weddingEnquiry.setCategory(EnquiryCategory.WEDDING);
        weddingEnquiry.setAssignedToUser(weddingStaff);
        weddingEnquiry.setStatus(EnquiryStatus.OPEN);

        when(enquiryRepository.findById(508L)).thenReturn(Optional.of(weddingEnquiry));

        // Unauthorized room staff attempts to reassign wedding enquiry
        RouteEnquiryRequest routeReq = new RouteEnquiryRequest(EnquiryCategory.ROOM_BOOKING, roomStaff.getId());
        assertThrows(AccessDeniedException.class, () ->
                enquiryService.routeEnquiry(508L, roomPrincipal, routeReq));

        // Verify save was NEVER called for this entity modification
        verify(enquiryRepository, never()).save(weddingEnquiry);
        assertEquals(EnquiryCategory.WEDDING, weddingEnquiry.getCategory());
        assertEquals(weddingStaff, weddingEnquiry.getAssignedToUser());
    }

    @Test
    @DisplayName("Edited draft body reconciles and sanitizes invented prices against authoritative quote data")
    void testEditedDraftSanitizesInventedPrices() {
        Enquiry roomEnquiry = new Enquiry();
        roomEnquiry.setId(509L);
        roomEnquiry.setEnquiryCode("ENQ-509");
        roomEnquiry.setCategory(EnquiryCategory.ROOM_BOOKING);
        roomEnquiry.setAssignedToUser(roomStaff);
        roomEnquiry.setStatus(EnquiryStatus.OPEN);
        roomEnquiry.setSourceChannel(EnquiryChannel.EMAIL_SIM);
        roomEnquiry.setSenderContact("guest509@example.com");

        EnquiryDraft pendingDraft = new EnquiryDraft();
        pendingDraft.setId(609L);
        pendingDraft.setEnquiry(roomEnquiry);
        pendingDraft.setDraftBody("Original AI draft");
        pendingDraft.setStatus(DraftStatus.DRAFT_PENDING_REVIEW);

        when(enquiryRepository.findById(509L)).thenReturn(Optional.of(roomEnquiry));
        when(enquiryDraftRepository.findFirstByEnquiryIdAndStatusOrderByCreatedAtDesc(509L, DraftStatus.DRAFT_PENDING_REVIEW))
                .thenReturn(Optional.of(pendingDraft));
        when(userRepository.findById(roomStaff.getId())).thenReturn(Optional.of(roomStaff));

        String inventedPriceBody = "Special promotional deal: Villa at only ₹2,000 per night all inclusive!";
        String sanitizedBody = "Special promotional deal: Villa at starting rate ₹10,500/night + 18% GST (Official Quote Breakdown)";

        when(quoteInjectorService.injectDatabaseQuote(eq(inventedPriceBody), eq(roomEnquiry)))
                .thenReturn(new QuoteInjectorService.InjectedQuote(sanitizedBody, null));
        when(simulatedDeliveryAdapter.deliver(any(), any()))
                .thenReturn(new SimulatedDeliveryAdapter.DeliveryResult("SIM-509", "EMAIL_SIM", "guest509@example.com", "DELIVERED_SIMULATED", LocalDateTime.now(), sanitizedBody));

        ApproveDraftRequest approveReq = new ApproveDraftRequest(inventedPriceBody);
        DraftApprovalResponse resp = enquiryService.approveDraft(509L, roomPrincipal, approveReq);

        assertNotNull(resp);
        assertEquals(sanitizedBody, pendingDraft.getDraftBody());
        assertEquals(sanitizedBody, resp.getDeliveredBody());
        verify(quoteInjectorService, times(1)).injectDatabaseQuote(eq(inventedPriceBody), eq(roomEnquiry));
    }

    @Test
    @DisplayName("Atomic draft claiming: when concurrent request claims draft first (0 rows updated), throws Conflict and never delivers")
    void testConcurrentApproval_AtomicClaimFails_ThrowsConflictAndPreventsDelivery() {
        Enquiry roomEnquiry = new Enquiry();
        roomEnquiry.setId(510L);
        roomEnquiry.setCategory(EnquiryCategory.ROOM_BOOKING);
        roomEnquiry.setAssignedToUser(roomStaff);
        roomEnquiry.setStatus(EnquiryStatus.OPEN);

        EnquiryDraft pendingDraft = new EnquiryDraft();
        pendingDraft.setId(610L);
        pendingDraft.setEnquiry(roomEnquiry);
        pendingDraft.setStatus(DraftStatus.DRAFT_PENDING_REVIEW);

        when(enquiryRepository.findById(510L)).thenReturn(Optional.of(roomEnquiry));
        when(enquiryDraftRepository.findFirstByEnquiryIdAndStatusOrderByCreatedAtDesc(510L, DraftStatus.DRAFT_PENDING_REVIEW))
                .thenReturn(Optional.of(pendingDraft));
        when(userRepository.findById(roomStaff.getId())).thenReturn(Optional.of(roomStaff));

        // Simulate concurrent atomic claim failure (0 rows updated)
        when(enquiryDraftRepository.atomicClaimDraft(eq(610L), eq(DraftStatus.DRAFT_PENDING_REVIEW), eq(DraftStatus.SENT_MANUALLY)))
                .thenReturn(0);

        ApproveDraftRequest approveReq = new ApproveDraftRequest("Losing thread attempt");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                enquiryService.approveDraft(510L, roomPrincipal, approveReq));

        assertTrue(ex.getMessage().contains("Draft approval conflict"));
        verify(simulatedDeliveryAdapter, never()).deliver(any(), any());
    }
}
