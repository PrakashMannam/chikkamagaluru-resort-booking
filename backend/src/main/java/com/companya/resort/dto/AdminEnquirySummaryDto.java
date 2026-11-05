package com.companya.resort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminEnquirySummaryDto {

    private Long id;

    @JsonProperty("enquiry_code")
    private String enquiryCode;

    @JsonProperty("source_channel")
    private String sourceChannel;

    @JsonProperty("sender_name")
    private String senderName;

    @JsonProperty("sender_contact")
    private String senderContact;

    @JsonProperty("ai_summary")
    private String aiSummary;

    private String category;

    @JsonProperty("ai_confidence")
    private BigDecimal aiConfidence;

    @JsonProperty("routing_method")
    private String routingMethod;

    @JsonProperty("extracted_check_in")
    private LocalDate extractedCheckIn;

    @JsonProperty("extracted_check_out")
    private LocalDate extractedCheckOut;

    @JsonProperty("extracted_nights")
    private Integer extractedNights;

    @JsonProperty("extracted_guests")
    private Integer extractedGuests;

    @JsonProperty("extracted_rooms")
    private Integer extractedRooms;

    @JsonProperty("extracted_budget_inr")
    private BigDecimal extractedBudgetInr;

    @JsonProperty("assigned_to_user_id")
    private Long assignedToUserId;

    @JsonProperty("assigned_to_user_name")
    private String assignedToUserName;

    @JsonProperty("assigned_to_user_lane")
    private String assignedToUserLane;

    @JsonProperty("gm_alerted")
    private Boolean gmAlerted;

    @JsonProperty("ai_error_status")
    private String aiErrorStatus;

    private String status;

    @JsonProperty("received_at")
    private LocalDateTime receivedAt;

    @JsonProperty("first_replied_at")
    private LocalDateTime firstRepliedAt;

    @JsonProperty("has_active_draft")
    private Boolean hasActiveDraft;

    public AdminEnquirySummaryDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEnquiryCode() {
        return enquiryCode;
    }

    public void setEnquiryCode(String enquiryCode) {
        this.enquiryCode = enquiryCode;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(String sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderContact() {
        return senderContact;
    }

    public void setSenderContact(String senderContact) {
        this.senderContact = senderContact;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(BigDecimal aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public String getRoutingMethod() {
        return routingMethod;
    }

    public void setRoutingMethod(String routingMethod) {
        this.routingMethod = routingMethod;
    }

    public LocalDate getExtractedCheckIn() {
        return extractedCheckIn;
    }

    public void setExtractedCheckIn(LocalDate extractedCheckIn) {
        this.extractedCheckIn = extractedCheckIn;
    }

    public LocalDate getExtractedCheckOut() {
        return extractedCheckOut;
    }

    public void setExtractedCheckOut(LocalDate extractedCheckOut) {
        this.extractedCheckOut = extractedCheckOut;
    }

    public Integer getExtractedNights() {
        return extractedNights;
    }

    public void setExtractedNights(Integer extractedNights) {
        this.extractedNights = extractedNights;
    }

    public Integer getExtractedGuests() {
        return extractedGuests;
    }

    public void setExtractedGuests(Integer extractedGuests) {
        this.extractedGuests = extractedGuests;
    }

    public Integer getExtractedRooms() {
        return extractedRooms;
    }

    public void setExtractedRooms(Integer extractedRooms) {
        this.extractedRooms = extractedRooms;
    }

    public BigDecimal getExtractedBudgetInr() {
        return extractedBudgetInr;
    }

    public void setExtractedBudgetInr(BigDecimal extractedBudgetInr) {
        this.extractedBudgetInr = extractedBudgetInr;
    }

    public Long getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(Long assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }

    public String getAssignedToUserName() {
        return assignedToUserName;
    }

    public void setAssignedToUserName(String assignedToUserName) {
        this.assignedToUserName = assignedToUserName;
    }

    public String getAssignedToUserLane() {
        return assignedToUserLane;
    }

    public void setAssignedToUserLane(String assignedToUserLane) {
        this.assignedToUserLane = assignedToUserLane;
    }

    public Boolean getGmAlerted() {
        return gmAlerted;
    }

    public void setGmAlerted(Boolean gmAlerted) {
        this.gmAlerted = gmAlerted;
    }

    public String getAiErrorStatus() {
        return aiErrorStatus;
    }

    public void setAiErrorStatus(String aiErrorStatus) {
        this.aiErrorStatus = aiErrorStatus;
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

    public LocalDateTime getFirstRepliedAt() {
        return firstRepliedAt;
    }

    public void setFirstRepliedAt(LocalDateTime firstRepliedAt) {
        this.firstRepliedAt = firstRepliedAt;
    }

    public Boolean getHasActiveDraft() {
        return hasActiveDraft;
    }

    public void setHasActiveDraft(Boolean hasActiveDraft) {
        this.hasActiveDraft = hasActiveDraft;
    }
}
