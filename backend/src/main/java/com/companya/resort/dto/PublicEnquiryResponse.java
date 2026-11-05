package com.companya.resort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class PublicEnquiryResponse {

    @JsonProperty("enquiry_code")
    private String enquiryCode;

    @JsonProperty("status")
    private String status;

    @JsonProperty("received_at")
    private LocalDateTime receivedAt;

    @JsonProperty("message")
    private String message;

    public PublicEnquiryResponse() {
    }

    public PublicEnquiryResponse(String enquiryCode, String status, LocalDateTime receivedAt, String message) {
        this.enquiryCode = enquiryCode;
        this.status = status;
        this.receivedAt = receivedAt;
        this.message = message;
    }

    public String getEnquiryCode() {
        return enquiryCode;
    }

    public void setEnquiryCode(String enquiryCode) {
        this.enquiryCode = enquiryCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
