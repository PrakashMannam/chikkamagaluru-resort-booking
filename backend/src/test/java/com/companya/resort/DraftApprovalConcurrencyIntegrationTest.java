package com.companya.resort;

import com.companya.resort.dto.ApproveDraftRequest;
import com.companya.resort.dto.DraftApprovalResponse;
import com.companya.resort.entity.*;
import com.companya.resort.repository.EnquiryDraftRepository;
import com.companya.resort.repository.EnquiryRepository;
import com.companya.resort.repository.UserRepository;
import com.companya.resort.security.UserPrincipal;
import com.companya.resort.service.EnquiryService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class DraftApprovalConcurrencyIntegrationTest {

    private static String testDbUrl;
    private static String testDbUser;
    private static String testDbPassword;
    private static ConfigurableApplicationContext applicationContext;

    private static EnquiryService enquiryService;
    private static EnquiryRepository enquiryRepository;
    private static EnquiryDraftRepository enquiryDraftRepository;
    private static UserRepository userRepository;

    private static User roomStaff;
    private static User weddingStaff;
    private static UserPrincipal roomPrincipal;
    private static UserPrincipal weddingPrincipal;

    @BeforeAll
    static void setUp() {
        testDbUrl = System.getenv("TEST_DB_URL");
        testDbUser = System.getenv("TEST_DB_USER");
        testDbPassword = System.getenv("TEST_DB_PASSWORD");

        if (testDbUrl == null || testDbUrl.isBlank()) {
            testDbUrl = "jdbc:mysql://localhost:3306/resort_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }
        if (testDbUser == null) {
            testDbUser = "root";
        }
        if (testDbPassword == null) {
            testDbPassword = "root";
        }

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(testDbUrl, testDbUser, testDbPassword)
                    .cleanDisabled(false)
                    .load();
            flyway.migrate();

            applicationContext = new SpringApplicationBuilder(ResortApplication.class)
                    .profiles("demo")
                    .web(org.springframework.boot.WebApplicationType.NONE)
                    .properties(
                            "DB_URL=" + testDbUrl,
                            "DB_USER=" + testDbUser,
                            "DB_PASSWORD=" + testDbPassword,
                            "spring.datasource.url=" + testDbUrl,
                            "spring.datasource.username=" + testDbUser,
                            "spring.datasource.password=" + testDbPassword,
                            "spring.jpa.hibernate.ddl-auto=none",
                            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
                            "spring.flyway.enabled=false",
                            "JWT_SECRET=supersecretkeyforresortbookingapp2026demo123456",
                            "CORS_ALLOWED_ORIGIN=http://localhost:5173"
                    )
                    .run();

            enquiryService = applicationContext.getBean(EnquiryService.class);
            enquiryRepository = applicationContext.getBean(EnquiryRepository.class);
            enquiryDraftRepository = applicationContext.getBean(EnquiryDraftRepository.class);
            userRepository = applicationContext.getBean(UserRepository.class);

            roomStaff = userRepository.findByEmail("rooms.demo@companya-estate.test")
                    .orElseGet(() -> userRepository.findAll().stream()
                            .filter(u -> u.getSpecialtyLane() == SpecialtyLane.ROOM)
                            .findFirst().orElseThrow());

            weddingStaff = userRepository.findByEmail("weddings.demo@companya-estate.test")
                    .orElseGet(() -> userRepository.findAll().stream()
                            .filter(u -> u.getSpecialtyLane() == SpecialtyLane.WEDDING)
                            .findFirst().orElseThrow());

            roomPrincipal = new UserPrincipal(roomStaff.getId(), roomStaff.getEmail(), roomStaff.getRole().name(), roomStaff.getIsGm(), roomStaff.getSpecialtyLane().name());
            weddingPrincipal = new UserPrincipal(weddingStaff.getId(), weddingStaff.getEmail(), weddingStaff.getRole().name(), weddingStaff.getIsGm(), weddingStaff.getSpecialtyLane().name());

        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Skipping DraftApprovalConcurrencyIntegrationTest: could not connect to MySQL resort_test (" + e.getMessage() + ")");
        }
    }

    @AfterAll
    static void tearDown() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    @Test
    @DisplayName("Two simultaneous approval requests against the same draft in MySQL: exactly one wins and transitions draft, one receives conflict")
    void testSimultaneousDraftApprovalConcurrency() throws Exception {
        // 1. Setup enquiry and draft in MySQL database
        Enquiry enquiry = new Enquiry();
        enquiry.setEnquiryCode("ENQ-CONCUR-" + System.currentTimeMillis());
        enquiry.setSenderName("Concurrent Guest");
        enquiry.setSenderContact("guest.concurrency@example.com");
        enquiry.setSourceChannel(EnquiryChannel.EMAIL_SIM);
        enquiry.setRawMessage("I would like to book 2 rooms for next month");
        enquiry.setCategory(EnquiryCategory.ROOM_BOOKING);
        enquiry.setRoutingMethod(RoutingMethod.AI_HIGH_CONFIDENCE);
        enquiry.setAssignedToUser(roomStaff);
        enquiry.setStatus(EnquiryStatus.OPEN);
        enquiry = enquiryRepository.save(enquiry);

        EnquiryDraft draft = new EnquiryDraft();
        draft.setEnquiry(enquiry);
        draft.setDraftBody("Initial AI Quote Draft");
        draft.setStatus(DraftStatus.DRAFT_PENDING_REVIEW);
        draft.setGeneratedBy(DraftGeneratedBy.AI_ROUTER);
        draft = enquiryDraftRepository.save(draft);

        final Long enquiryId = enquiry.getId();
        final Long draftId = draft.getId();

        // 2. Prepare two simultaneous threads with countdown latches
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<DraftApprovalResponse>> futures = new ArrayList<>();

        for (int i = 1; i <= threadCount; i++) {
            final String customizedBody = "Approved customized message from thread " + i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                // Wait for both threads to reach the starting barrier
                startLatch.await(5, TimeUnit.SECONDS);
                return enquiryService.approveDraft(enquiryId, roomPrincipal, new ApproveDraftRequest(customizedBody));
            }));
        }

        // Wait until both threads are ready, then release both simultaneously
        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "Threads failed to prepare in time");
        startLatch.countDown();

        int successCount = 0;
        int conflictCount = 0;
        DraftApprovalResponse successfulResponse = null;

        for (Future<DraftApprovalResponse> future : futures) {
            try {
                DraftApprovalResponse resp = future.get(10, TimeUnit.SECONDS);
                successCount++;
                successfulResponse = resp;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IllegalStateException) {
                    conflictCount++;
                    assertTrue(cause.getMessage().contains("conflict") || cause.getMessage().contains("No pending draft"),
                            "Conflict message must explain that draft was already claimed: " + cause.getMessage());
                } else {
                    fail("Unexpected exception: " + cause);
                }
            }
        }

        executor.shutdown();

        // 3. Assertions
        assertEquals(1, successCount, "Exactly one approval request must succeed");
        assertEquals(1, conflictCount, "Exactly one approval request must receive conflict (IllegalStateException)");
        assertNotNull(successfulResponse, "Winning response must not be null");

        // Verify final draft status in database
        EnquiryDraft persistedDraft = enquiryDraftRepository.findById(draftId).orElseThrow();
        assertEquals(DraftStatus.SENT_MANUALLY, persistedDraft.getStatus(), "Draft status must be SENT_MANUALLY");
        assertEquals(roomStaff.getId(), persistedDraft.getSentByUser().getId());
        assertNotNull(persistedDraft.getSentAt());

        // Verify winning message body is persisted
        assertEquals(successfulResponse.getDeliveredBody(), persistedDraft.getDraftBody(),
                "Persisted draft body must match the winning approved response");

        // Verify enquiry status in database
        Enquiry persistedEnquiry = enquiryRepository.findById(enquiryId).orElseThrow();
        assertEquals(EnquiryStatus.REPLIED, persistedEnquiry.getStatus(), "Enquiry status must transition to REPLIED");
        assertNotNull(persistedEnquiry.getFirstRepliedAt());
    }

    @Test
    @DisplayName("Unauthorized staff cannot trigger draft approval or delivery, leaving draft pending in DB")
    void testUnauthorizedRequestCannotTriggerDelivery() {
        Enquiry enquiry = new Enquiry();
        enquiry.setEnquiryCode("ENQ-UNAUTH-" + System.currentTimeMillis());
        enquiry.setSenderName("Wedding Lead");
        enquiry.setSenderContact("wedding@example.com");
        enquiry.setSourceChannel(EnquiryChannel.EMAIL_SIM);
        enquiry.setRawMessage("Inquiry about room booking");
        enquiry.setCategory(EnquiryCategory.ROOM_BOOKING);
        enquiry.setRoutingMethod(RoutingMethod.AI_HIGH_CONFIDENCE);
        enquiry.setAssignedToUser(roomStaff);
        enquiry.setStatus(EnquiryStatus.OPEN);
        enquiry = enquiryRepository.save(enquiry);

        EnquiryDraft draft = new EnquiryDraft();
        draft.setEnquiry(enquiry);
        draft.setDraftBody("Room draft pending");
        draft.setStatus(DraftStatus.DRAFT_PENDING_REVIEW);
        draft.setGeneratedBy(DraftGeneratedBy.AI_ROUTER);
        draft = enquiryDraftRepository.save(draft);

        final Long enquiryId = enquiry.getId();
        final Long draftId = draft.getId();

        // Wedding staff attempts to approve room booking draft
        assertThrows(AccessDeniedException.class, () ->
                enquiryService.approveDraft(enquiryId, weddingPrincipal, new ApproveDraftRequest("Unauthorized wedding approval")));

        // Verify draft remains untouched in DB
        EnquiryDraft persistedDraft = enquiryDraftRepository.findById(draftId).orElseThrow();
        assertEquals(DraftStatus.DRAFT_PENDING_REVIEW, persistedDraft.getStatus(), "Draft status must remain DRAFT_PENDING_REVIEW");
        assertNull(persistedDraft.getSentByUser());
        assertNull(persistedDraft.getSentAt());

        Enquiry persistedEnquiry = enquiryRepository.findById(enquiryId).orElseThrow();
        assertEquals(EnquiryStatus.OPEN, persistedEnquiry.getStatus(), "Enquiry status must remain OPEN");
    }
}
