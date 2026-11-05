package com.companya.resort.controller;

import com.companya.resort.dto.CreateEnquiryRequest;
import com.companya.resort.dto.PublicEnquiryResponse;
import com.companya.resort.service.EnquiryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/enquiries")
public class PublicEnquiryController {

    private final EnquiryService enquiryService;

    public PublicEnquiryController(EnquiryService enquiryService) {
        this.enquiryService = enquiryService;
    }

    @PostMapping
    public ResponseEntity<PublicEnquiryResponse> submitPublicEnquiry(@Valid @RequestBody CreateEnquiryRequest request) {
        PublicEnquiryResponse response = enquiryService.createPublicEnquiry(request);
        return ResponseEntity.ok(response);
    }
}
