package com.companya.resort.service;

import com.companya.resort.dto.AdminEnquiryDetailDto;
import com.companya.resort.dto.AdminEnquirySummaryDto;
import com.companya.resort.dto.ApproveDraftRequest;
import com.companya.resort.dto.CreateEnquiryRequest;
import com.companya.resort.dto.DraftApprovalResponse;
import com.companya.resort.dto.GeminiEnquiryExtraction;
import com.companya.resort.dto.PublicEnquiryResponse;
import com.companya.resort.dto.RouteEnquiryRequest;
import com.companya.resort.dto.SimulateEnquiryRequest;
import com.companya.resort.entity.DraftGeneratedBy;
import com.companya.resort.entity.DraftStatus;
import com.companya.resort.entity.Enquiry;
import com.companya.resort.entity.EnquiryCategory;
import com.companya.resort.entity.EnquiryChannel;
import com.companya.resort.entity.EnquiryDraft;
import com.companya.resort.entity.EnquiryStatus;
import com.companya.resort.entity.RoutingMethod;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import com.companya.resort.exception.ResourceNotFoundException;
import com.companya.resort.repository.EnquiryDraftRepository;
import com.companya.resort.repository.EnquiryRepository;
import com.companya.resort.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnquiryService {

    private static final Logger log = LoggerFactory.getLogger(EnquiryService.class);
    private static final String ENQ_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom secureRandom = new SecureRandom();

    private final EnquiryRepository enquiryRepository;
    private final EnquiryDraftRepository enquiryDraftRepository;
    private final UserRepository userRepository;
    private final GeminiAiService geminiAiService;
    private final RuleFallbackService ruleFallbackService;
    private final EnquiryRoutingService enquiryRoutingService;
    private final QuoteInjectorService quoteInjectorService;
    private final SimulatedDeliveryAdapter simulatedDeliveryAdapter;

    private final BigDecimal confidenceThreshold;

    public EnquiryService(
            EnquiryRepository enquiryRepository,
            EnquiryDraftRepository enquiryDraftRepository,
            UserRepository userRepository,
            GeminiAiService geminiAiService,
            RuleFallbackService ruleFallbackService,
            EnquiryRoutingService enquiryRoutingService,
            QuoteInjectorService quoteInjectorService,
            SimulatedDeliveryAdapter simulatedDeliveryAdapter,
            @Value("${enquiry.ai.confidence-threshold:0.80}") BigDecimal confidenceThreshold
    ) {
        this.enquiryRepository = enquiryRepository;
        this.enquiryDraftRepository = enquiryDraftRepository;
        this.userRepository = userRepository;
        this.geminiAiService = geminiAiService;
        this.ruleFallbackService = ruleFallbackService;
        this.enquiryRoutingService = enquiryRoutingService;
        this.quoteInjectorService = quoteInjectorService;
        this.simulatedDeliveryAdapter = simulatedDeliveryAdapter;
        this.confidenceThreshold = confidenceThreshold != null ? confidenceThreshold : new BigDecimal("0.80");
    }

    @Transactional
    public PublicEnquiryResponse createPublicEnquiry(CreateEnquiryRequest request) {
        Enquiry enquiry = new Enquiry();
        enquiry.setEnquiryCode(generateUniqueEnquiryCode());
        enquiry.setSourceChannel(EnquiryChannel.WEB_FORM);
        enquiry.setSenderName(request.getSenderName());
        enquiry.setSenderContact(request.getSenderContact());
        enquiry.setRawMessage(request.getRawMessage());
        enquiry.setStatus(EnquiryStatus.OPEN);
        enquiry.setReceivedAt(LocalDateTime.now());
        enquiry.setCategory(EnquiryCategory.UNSORTED);
        enquiry.setRoutingMethod(RoutingMethod.RULE_FALLBACK);
        enquiry.setGmAlerted(false);

        // 1. Preserve initial enquiry immediately
        enquiry = enquiryRepository.save(enquiry);

        // 2. Process via AI / fallback pipeline
        processEnquiryPipeline(enquiry);

        return new PublicEnquiryResponse(
                enquiry.getEnquiryCode(),
                enquiry.getStatus().name(),
                enquiry.getReceivedAt(),
                "Thank you! Your enquiry has been received and our team will get in touch with you shortly."
        );
    }

    @Transactional
    public AdminEnquiryDetailDto simulateEnquiry(SimulateEnquiryRequest request) {
        Enquiry enquiry = new Enquiry();
        enquiry.setEnquiryCode(generateUniqueEnquiryCode());
        enquiry.setSourceChannel(request.getSourceChannel() != null ? request.getSourceChannel() : EnquiryChannel.EMAIL_SIM);
        enquiry.setSenderName(request.getSenderName());
        enquiry.setSenderContact(request.getSenderContact());
        enquiry.setRawMessage(request.getRawMessage());
        enquiry.setStatus(EnquiryStatus.OPEN);
        enquiry.setReceivedAt(LocalDateTime.now());
        enquiry.setCategory(EnquiryCategory.UNSORTED);
        enquiry.setRoutingMethod(RoutingMethod.RULE_FALLBACK);
        enquiry.setGmAlerted(false);

        // 1. Preserve initial enquiry immediately
        enquiry = enquiryRepository.save(enquiry);

        // 2. Process via AI / fallback pipeline
        processEnquiryPipeline(enquiry);

        return getEnquiryDetails(enquiry.getId());
    }

    private void processEnquiryPipeline(Enquiry enquiry) {
        GeminiAiService.AiProcessingResult aiResult;
        try {
            aiResult = geminiAiService.processEnquiry(enquiry.getRawMessage());
        } catch (Exception e) {
            log.error("Unexpected exception invoking Gemini AI service: {}", e.getMessage());
            aiResult = GeminiAiService.AiProcessingResult.failure("UNEXPECTED_ERROR", e.getMessage());
        }

        if (aiResult.isSuccess()) {
            GeminiEnquiryExtraction extraction = aiResult.extraction();
            applyExtractionToEnquiry(enquiry, extraction);
            enquiryRoutingService.assignStaff(enquiry);
            enquiryRepository.save(enquiry);

            // Generate sanitized, database-quoted draft
            String suggestedReply = extraction.getSuggestedReply();
            QuoteInjectorService.InjectedQuote quote = quoteInjectorService.injectDatabaseQuote(suggestedReply, enquiry);

            EnquiryDraft draft = new EnquiryDraft();
            draft.setEnquiry(enquiry);
            draft.setDraftBody(quote.finalDraftBody());
            draft.setQuotedRateBreakdownJson(quote.quotedRateBreakdownJson());
            draft.setGeneratedBy(DraftGeneratedBy.AI_ROUTER);
            draft.setStatus(DraftStatus.DRAFT_PENDING_REVIEW);
            draft.setCreatedAt(LocalDateTime.now());
            enquiryDraftRepository.save(draft);

        } else {
            // Failure handling: apply deterministic rule fallback
            log.warn("AI processing failed with status={}. Applying deterministic fallback.", aiResult.errorStatus());
            enquiry.setAiErrorStatus(aiResult.errorStatus());
            enquiry.setRoutingMethod(RoutingMethod.RULE_FALLBACK);

            // Attempt rule-based keyword extraction for classification and stay parameters
            GeminiEnquiryExtraction ruleExt = ruleFallbackService.classifyFallback(enquiry.getRawMessage());

            if (EnquiryCategory.COMPLAINT.name().equalsIgnoreCase(ruleExt.getCategory())) {
                enquiry.setCategory(EnquiryCategory.COMPLAINT);
                enquiry.setGmAlerted(true);
                enquiry.setAiConfidence(ruleExt.getConfidence() != null ? ruleExt.getConfidence() : new BigDecimal("0.85"));
                enquiry.setAiSummary(ruleExt.getSummary() != null ? ruleExt.getSummary() : "Deterministic fallback: Guest complaint flagged for General Manager review.");
            } else {
                // Keep ambiguous or ordinary failure messages in UNSORTED queue
                enquiry.setCategory(EnquiryCategory.UNSORTED);
                enquiry.setGmAlerted(false);
                enquiry.setAiConfidence(BigDecimal.ZERO);
                enquiry.setAiSummary("AI extraction failed (" + aiResult.errorStatus() + "). Unsorted queue for staff review.");
            }

            if (ruleExt.getGuests() != null) enquiry.setExtractedGuests(ruleExt.getGuests());
            if (ruleExt.getNights() != null) enquiry.setExtractedNights(ruleExt.getNights());
            if (ruleExt.getRooms() != null) enquiry.setExtractedRooms(ruleExt.getRooms());
            if (ruleExt.getBudgetAmount() != null) enquiry.setExtractedBudgetInr(ruleExt.getBudgetAmount());

            enquiryRoutingService.assignStaff(enquiry);
            enquiryRepository.save(enquiry);

            // Create fallback template draft for staff review
            String templateReply;
            if (enquiry.getCategory() == EnquiryCategory.COMPLAINT) {
                templateReply = ruleExt.getSuggestedReply() != null
                        ? ruleExt.getSuggestedReply()
                        : "Dear Guest,\n\nWe sincerely apologize for your experience. Your feedback has been escalated directly to our General Manager, who will personally follow up with you.\n\nWarm regards,\nManagement Team";
            } else {
                templateReply = "Dear Guest,\n\nThank you for reaching out to us. We have received your enquiry and our reservations team will get in touch with you shortly with full details.\n\nWarm regards,\nEstate Reservations Team";
            }

            QuoteInjectorService.InjectedQuote quote = quoteInjectorService.injectDatabaseQuote(templateReply, enquiry);

            EnquiryDraft draft = new EnquiryDraft();
            draft.setEnquiry(enquiry);
            draft.setDraftBody(quote.finalDraftBody());
            draft.setQuotedRateBreakdownJson(quote.quotedRateBreakdownJson());
            draft.setGeneratedBy(DraftGeneratedBy.RULE_TEMPLATE);
            draft.setStatus(DraftStatus.DRAFT_PENDING_REVIEW);
            draft.setCreatedAt(LocalDateTime.now());
            enquiryDraftRepository.save(draft);
        }
    }

    private void applyExtractionToEnquiry(Enquiry enquiry, GeminiEnquiryExtraction extraction) {
        if (extraction.getSummary() != null) {
            enquiry.setAiSummary(extraction.getSummary());
        }

        // Dates
        if (extraction.getCheckInDate() != null) {
            try {
                enquiry.setExtractedCheckIn(LocalDate.parse(extraction.getCheckInDate().trim()));
            } catch (DateTimeParseException ignored) {}
        }
        if (extraction.getCheckOutDate() != null) {
            try {
                enquiry.setExtractedCheckOut(LocalDate.parse(extraction.getCheckOutDate().trim()));
            } catch (DateTimeParseException ignored) {}
        }

        enquiry.setExtractedNights(extraction.getNights());
        enquiry.setExtractedGuests(extraction.getGuests());
        enquiry.setExtractedRooms(extraction.getRooms());
        enquiry.setExtractedBudgetInr(extraction.getBudgetAmount());

        BigDecimal confidence = extraction.getConfidence() != null ? extraction.getConfidence() : BigDecimal.ZERO;
        enquiry.setAiConfidence(confidence);

        EnquiryCategory cat = EnquiryCategory.UNSORTED;
        if (extraction.getCategory() != null) {
            try {
                cat = EnquiryCategory.valueOf(extraction.getCategory().toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {}
        }

        // Confidence threshold check
        if (confidence.compareTo(confidenceThreshold) < 0) {
            enquiry.setCategory(EnquiryCategory.UNSORTED);
            enquiry.setRoutingMethod(RoutingMethod.AI_LOW_CONFIDENCE_GM);
        } else {
            enquiry.setCategory(cat);
            enquiry.setRoutingMethod(RoutingMethod.AI_HIGH_CONFIDENCE);
        }
    }

    public boolean canAccessEnquiry(com.companya.resort.security.UserPrincipal principal, Enquiry enquiry) {
        if (principal == null) {
            return true;
        }
        if (principal.isGm()) {
            return true;
        }
        if (enquiry.getAssignedToUser() != null && enquiry.getAssignedToUser().getId().equals(principal.getId())) {
            return true;
        }
        SpecialtyLane userLane = parseSpecialtyLane(principal.getSpecialtyLane());
        if (userLane == null) {
            return false;
        }
        return isCategoryPermittedForLane(enquiry.getCategory(), userLane);
    }

    public boolean isCategoryPermittedForLane(EnquiryCategory category, SpecialtyLane lane) {
        if (category == null || lane == null) {
            return false;
        }
        if (category == EnquiryCategory.COMPLAINT || category == EnquiryCategory.UNSORTED) {
            return false;
        }
        return switch (lane) {
            case ROOM -> category == EnquiryCategory.ROOM_BOOKING;
            case WEDDING -> category == EnquiryCategory.WEDDING;
            case OFFSITE -> category == EnquiryCategory.CORPORATE_OFFSITE;
            case GM_ESCALATION -> true;
        };
    }

    private SpecialtyLane parseSpecialtyLane(String laneStr) {
        if (laneStr == null || laneStr.isBlank()) return null;
        try {
            return SpecialtyLane.valueOf(laneStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<AdminEnquirySummaryDto> listEnquiries(
            com.companya.resort.security.UserPrincipal principal,
            EnquiryCategory category,
            EnquiryStatus status,
            Long assignedUserId,
            EnquiryChannel channel,
            BigDecimal minConfidence
    ) {
        boolean isGm = (principal == null || principal.isGm());
        SpecialtyLane userLane = (principal != null) ? parseSpecialtyLane(principal.getSpecialtyLane()) : null;

        if (!isGm) {
            if (category == EnquiryCategory.COMPLAINT || category == EnquiryCategory.UNSORTED) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Unauthorized: Access to complaints and unsorted enquiries is restricted to General Managers"
                );
            }
            if (category != null && !isCategoryPermittedForLane(category, userLane)) {
                return List.of();
            }
        }

        Specification<Enquiry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            } else if (!isGm && userLane != null) {
                EnquiryCategory permittedCat = switch (userLane) {
                    case ROOM -> EnquiryCategory.ROOM_BOOKING;
                    case WEDDING -> EnquiryCategory.WEDDING;
                    case OFFSITE -> EnquiryCategory.CORPORATE_OFFSITE;
                    case GM_ESCALATION -> null;
                };

                Predicate laneMatch = (permittedCat != null) ? cb.equal(root.get("category"), permittedCat) : cb.disjunction();
                Predicate assignedMatch = (principal.getId() != null) ? cb.equal(root.get("assignedToUser").get("id"), principal.getId()) : cb.disjunction();
                Predicate notComplaintOrUnsorted = cb.not(root.get("category").in(EnquiryCategory.COMPLAINT, EnquiryCategory.UNSORTED));

                predicates.add(cb.and(cb.or(laneMatch, assignedMatch), notComplaintOrUnsorted));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (assignedUserId != null) {
                predicates.add(cb.equal(root.get("assignedToUser").get("id"), assignedUserId));
            }
            if (channel != null) {
                predicates.add(cb.equal(root.get("sourceChannel"), channel));
            }
            if (minConfidence != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("aiConfidence"), minConfidence));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return enquiryRepository.findAll(spec)
                .stream()
                .sorted((a, b) -> b.getReceivedAt().compareTo(a.getReceivedAt()))
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AdminEnquirySummaryDto> listEnquiries(
            EnquiryCategory category,
            EnquiryStatus status,
            Long assignedUserId,
            EnquiryChannel channel,
            BigDecimal minConfidence
    ) {
        return listEnquiries(null, category, status, assignedUserId, channel, minConfidence);
    }

    @Transactional(readOnly = true)
    public AdminEnquiryDetailDto getEnquiryDetails(Long id, com.companya.resort.security.UserPrincipal principal) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + id));

        if (!canAccessEnquiry(principal, enquiry)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unauthorized: You do not have permission to view enquiries outside your assigned specialty lane"
            );
        }

        EnquiryDraft activeDraft = enquiryDraftRepository.findFirstByEnquiryIdAndStatusOrderByCreatedAtDesc(id, DraftStatus.DRAFT_PENDING_REVIEW)
                .or(() -> enquiryDraftRepository.findFirstByEnquiryIdOrderByCreatedAtDesc(id))
                .orElse(null);

        return toDetailDto(enquiry, activeDraft);
    }

    @Transactional(readOnly = true)
    public AdminEnquiryDetailDto getEnquiryDetails(Long id) {
        return getEnquiryDetails(id, null);
    }

    @Transactional
    public AdminEnquiryDetailDto routeEnquiry(Long id, com.companya.resort.security.UserPrincipal principal, RouteEnquiryRequest request) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + id));

        if (!canAccessEnquiry(principal, enquiry)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unauthorized: You cannot reroute an enquiry outside your assigned specialty lane"
            );
        }

        if (principal != null && !principal.isGm()) {
            if (request.getCategory() == EnquiryCategory.COMPLAINT || request.getCategory() == EnquiryCategory.UNSORTED) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Unauthorized: Only the General Manager can route enquiries to Complaint or Unsorted"
                );
            }
        }

        if (request.getCategory() != null) {
            enquiry.setCategory(request.getCategory());
            if (request.getCategory() == EnquiryCategory.COMPLAINT) {
                enquiry.setGmAlerted(true);
            }
        }

        if (request.getAssignedToUserId() != null) {
            User staff = userRepository.findById(request.getAssignedToUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getAssignedToUserId()));
            enquiry.setAssignedToUser(staff);
        } else if (request.getCategory() != null) {
            // Auto reassign staff based on new category
            enquiryRoutingService.assignStaff(enquiry);
        }

        enquiry.setRoutingMethod(RoutingMethod.MANUAL_OVERRIDE);
        enquiryRepository.save(enquiry);

        return getEnquiryDetails(id, principal);
    }

    @Transactional
    public AdminEnquiryDetailDto routeEnquiry(Long id, RouteEnquiryRequest request) {
        return routeEnquiry(id, null, request);
    }

    @Transactional
    public DraftApprovalResponse approveDraft(Long enquiryId, com.companya.resort.security.UserPrincipal principal, ApproveDraftRequest request) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        if (!canAccessEnquiry(principal, enquiry)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unauthorized: You cannot approve drafts outside your assigned specialty lane"
            );
        }

        Long staffUserId = (principal != null) ? principal.getId() : null;
        return executeDraftApproval(enquiry, staffUserId, request);
    }

    @Transactional
    public DraftApprovalResponse approveDraft(Long enquiryId, Long staffUserId, ApproveDraftRequest request) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));
        return executeDraftApproval(enquiry, staffUserId, request);
    }

    private DraftApprovalResponse executeDraftApproval(Enquiry enquiry, Long staffUserId, ApproveDraftRequest request) {
        Long enquiryId = enquiry.getId();
        EnquiryDraft draft = enquiryDraftRepository.findFirstByEnquiryIdAndStatusOrderByCreatedAtDesc(enquiryId, DraftStatus.DRAFT_PENDING_REVIEW)
                .or(() -> enquiryDraftRepository.findFirstByEnquiryIdOrderByCreatedAtDesc(enquiryId))
                .orElseThrow(() -> new ResourceNotFoundException("No draft found for enquiry id: " + enquiryId));

        if (request != null && request.getDraftBody() != null && !request.getDraftBody().isBlank()) {
            draft.setDraftBody(request.getDraftBody());
        }

        if (staffUserId != null) {
            userRepository.findById(staffUserId).ifPresent(draft::setSentByUser);
        }

        draft.setStatus(DraftStatus.SENT_MANUALLY);
        draft.setSentAt(LocalDateTime.now());
        enquiryDraftRepository.save(draft);

        enquiry.setStatus(EnquiryStatus.REPLIED);
        if (enquiry.getFirstRepliedAt() == null) {
            enquiry.setFirstRepliedAt(LocalDateTime.now());
        }
        enquiryRepository.save(enquiry);

        // Invoke simulated delivery
        SimulatedDeliveryAdapter.DeliveryResult deliveryResult = simulatedDeliveryAdapter.deliver(enquiry, draft);

        return new DraftApprovalResponse(
                enquiry.getId(),
                enquiry.getEnquiryCode(),
                draft.getId(),
                enquiry.getStatus().name(),
                draft.getStatus().name(),
                deliveryResult.recipient(),
                deliveryResult.channel(),
                draft.getDraftBody(),
                deliveryResult.dispatchedAt()
        );
    }

    @Transactional
    public AdminEnquiryDetailDto discardDraft(Long enquiryId, com.companya.resort.security.UserPrincipal principal) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        if (!canAccessEnquiry(principal, enquiry)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unauthorized: You cannot discard drafts outside your assigned specialty lane"
            );
        }

        EnquiryDraft draft = enquiryDraftRepository.findFirstByEnquiryIdAndStatusOrderByCreatedAtDesc(enquiryId, DraftStatus.DRAFT_PENDING_REVIEW)
                .orElseThrow(() -> new ResourceNotFoundException("No pending draft found for enquiry id: " + enquiryId));

        draft.setStatus(DraftStatus.DISCARDED);
        enquiryDraftRepository.save(draft);

        return getEnquiryDetails(enquiryId, principal);
    }

    private AdminEnquirySummaryDto toSummaryDto(Enquiry enquiry) {
        AdminEnquirySummaryDto dto = new AdminEnquirySummaryDto();
        dto.setId(enquiry.getId());
        dto.setEnquiryCode(enquiry.getEnquiryCode());
        dto.setSourceChannel(enquiry.getSourceChannel() != null ? enquiry.getSourceChannel().name() : null);
        dto.setSenderName(enquiry.getSenderName());
        dto.setSenderContact(enquiry.getSenderContact());
        dto.setAiSummary(enquiry.getAiSummary());
        dto.setCategory(enquiry.getCategory() != null ? enquiry.getCategory().name() : null);
        dto.setAiConfidence(enquiry.getAiConfidence());
        dto.setRoutingMethod(enquiry.getRoutingMethod() != null ? enquiry.getRoutingMethod().name() : null);
        dto.setExtractedCheckIn(enquiry.getExtractedCheckIn());
        dto.setExtractedCheckOut(enquiry.getExtractedCheckOut());
        dto.setExtractedNights(enquiry.getExtractedNights());
        dto.setExtractedGuests(enquiry.getExtractedGuests());
        dto.setExtractedRooms(enquiry.getExtractedRooms());
        dto.setExtractedBudgetInr(enquiry.getExtractedBudgetInr());
        dto.setGmAlerted(enquiry.getGmAlerted());
        dto.setAiErrorStatus(enquiry.getAiErrorStatus());
        dto.setStatus(enquiry.getStatus() != null ? enquiry.getStatus().name() : null);
        dto.setReceivedAt(enquiry.getReceivedAt());
        dto.setFirstRepliedAt(enquiry.getFirstRepliedAt());

        if (enquiry.getAssignedToUser() != null) {
            dto.setAssignedToUserId(enquiry.getAssignedToUser().getId());
            dto.setAssignedToUserName(enquiry.getAssignedToUser().getFullName());
            dto.setAssignedToUserLane(enquiry.getAssignedToUser().getSpecialtyLane() != null ?
                    enquiry.getAssignedToUser().getSpecialtyLane().name() : null);
        }

        boolean hasActive = enquiryDraftRepository.findFirstByEnquiryIdAndStatusOrderByCreatedAtDesc(enquiry.getId(), DraftStatus.DRAFT_PENDING_REVIEW).isPresent();
        dto.setHasActiveDraft(hasActive);

        return dto;
    }

    private AdminEnquiryDetailDto toDetailDto(Enquiry enquiry, EnquiryDraft draft) {
        AdminEnquiryDetailDto dto = new AdminEnquiryDetailDto();
        dto.setId(enquiry.getId());
        dto.setEnquiryCode(enquiry.getEnquiryCode());
        dto.setSourceChannel(enquiry.getSourceChannel() != null ? enquiry.getSourceChannel().name() : null);
        dto.setSenderName(enquiry.getSenderName());
        dto.setSenderContact(enquiry.getSenderContact());
        dto.setRawMessage(enquiry.getRawMessage());
        dto.setAiSummary(enquiry.getAiSummary());
        dto.setCategory(enquiry.getCategory() != null ? enquiry.getCategory().name() : null);
        dto.setAiConfidence(enquiry.getAiConfidence());
        dto.setRoutingMethod(enquiry.getRoutingMethod() != null ? enquiry.getRoutingMethod().name() : null);
        dto.setExtractedCheckIn(enquiry.getExtractedCheckIn());
        dto.setExtractedCheckOut(enquiry.getExtractedCheckOut());
        dto.setExtractedNights(enquiry.getExtractedNights());
        dto.setExtractedGuests(enquiry.getExtractedGuests());
        dto.setExtractedRooms(enquiry.getExtractedRooms());
        dto.setExtractedBudgetInr(enquiry.getExtractedBudgetInr());
        dto.setGmAlerted(enquiry.getGmAlerted());
        dto.setAiErrorStatus(enquiry.getAiErrorStatus());
        dto.setStatus(enquiry.getStatus() != null ? enquiry.getStatus().name() : null);
        dto.setReceivedAt(enquiry.getReceivedAt());
        dto.setFirstRepliedAt(enquiry.getFirstRepliedAt());

        if (enquiry.getAssignedToUser() != null) {
            dto.setAssignedToUserId(enquiry.getAssignedToUser().getId());
            dto.setAssignedToUserName(enquiry.getAssignedToUser().getFullName());
            dto.setAssignedToUserLane(enquiry.getAssignedToUser().getSpecialtyLane() != null ?
                    enquiry.getAssignedToUser().getSpecialtyLane().name() : null);
        }

        if (draft != null) {
            AdminEnquiryDetailDto.ActiveDraftDto draftDto = new AdminEnquiryDetailDto.ActiveDraftDto();
            draftDto.setId(draft.getId());
            draftDto.setDraftBody(draft.getDraftBody());
            draftDto.setStatus(draft.getStatus() != null ? draft.getStatus().name() : null);
            draftDto.setQuotedRateBreakdownJson(draft.getQuotedRateBreakdownJson());
            draftDto.setGeneratedBy(draft.getGeneratedBy() != null ? draft.getGeneratedBy().name() : null);
            if (draft.getSentByUser() != null) {
                draftDto.setSentByUserId(draft.getSentByUser().getId());
                draftDto.setSentByUserName(draft.getSentByUser().getFullName());
            }
            draftDto.setSentAt(draft.getSentAt());
            draftDto.setCreatedAt(draft.getCreatedAt());
            dto.setActiveDraft(draftDto);
        }

        return dto;
    }

    private String generateUniqueEnquiryCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("ENQ-");
            for (int i = 0; i < 4; i++) {
                sb.append(ENQ_CODE_CHARS.charAt(secureRandom.nextInt(ENQ_CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!enquiryRepository.existsByEnquiryCode(code)) {
                return code;
            }
        }
        return "ENQ-" + System.currentTimeMillis() % 100000;
    }
}
