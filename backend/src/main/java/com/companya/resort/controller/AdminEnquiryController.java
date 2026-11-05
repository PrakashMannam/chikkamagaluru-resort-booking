package com.companya.resort.controller;

import com.companya.resort.dto.AdminEnquiryDetailDto;
import com.companya.resort.dto.AdminEnquirySummaryDto;
import com.companya.resort.dto.ApproveDraftRequest;
import com.companya.resort.dto.DraftApprovalResponse;
import com.companya.resort.dto.RouteEnquiryRequest;
import com.companya.resort.dto.SimulateEnquiryRequest;
import com.companya.resort.entity.EnquiryCategory;
import com.companya.resort.entity.EnquiryChannel;
import com.companya.resort.entity.EnquiryStatus;
import com.companya.resort.security.UserPrincipal;
import com.companya.resort.service.EnquiryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/enquiries")
public class AdminEnquiryController {

    private final EnquiryService enquiryService;

    public AdminEnquiryController(EnquiryService enquiryService) {
        this.enquiryService = enquiryService;
    }

    @PostMapping("/simulate")
    public ResponseEntity<AdminEnquiryDetailDto> simulateEnquiry(@Valid @RequestBody SimulateEnquiryRequest request) {
        AdminEnquiryDetailDto response = enquiryService.simulateEnquiry(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AdminEnquirySummaryDto>> listEnquiries(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) EnquiryCategory category,
            @RequestParam(required = false) EnquiryStatus status,
            @RequestParam(value = "assigned_to", required = false) Long assignedUserId,
            @RequestParam(required = false) EnquiryChannel channel,
            @RequestParam(value = "min_confidence", required = false) BigDecimal minConfidence
    ) {
        List<AdminEnquirySummaryDto> results = enquiryService.listEnquiries(principal, category, status, assignedUserId, channel, minConfidence);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminEnquiryDetailDto> getEnquiryDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AdminEnquiryDetailDto response = enquiryService.getEnquiryDetails(id, principal);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/route")
    public ResponseEntity<AdminEnquiryDetailDto> routeEnquiry(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody RouteEnquiryRequest request
    ) {
        AdminEnquiryDetailDto response = enquiryService.routeEnquiry(id, principal, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve-draft")
    public ResponseEntity<DraftApprovalResponse> approveDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) ApproveDraftRequest request
    ) {
        DraftApprovalResponse response = enquiryService.approveDraft(id, principal, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/discard-draft")
    public ResponseEntity<AdminEnquiryDetailDto> discardDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AdminEnquiryDetailDto response = enquiryService.discardDraft(id, principal);
        return ResponseEntity.ok(response);
    }
}
