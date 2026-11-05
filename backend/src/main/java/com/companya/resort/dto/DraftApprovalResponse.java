package com.companya.resort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class DraftApprovalResponse {

    @JsonProperty("enquiry_id")
    private Long enquiryId;

    @JsonProperty("enquiry_code")
    private String enquiryCode;

    @JsonProperty("draft_id")
    private Long draftId;

    @JsonProperty("enquiry_status")
    private String enquiryStatus;

    @JsonProperty("draft_status")
    private String draftStatus;

    @JsonProperty("delivered_to")
    private String deliveredTo;

    @JsonProperty("delivery_channel")
    private String deliveryChannel;

    @JsonProperty("delivered_body")
    private String deliveredBody;

    @JsonProperty("delivered_at")
    private LocalDateTime deliveredAt;

    public DraftApprovalResponse() {
    }

    public DraftApprovalResponse(Long enquiryId, String enquiryCode, Long draftId, String enquiryStatus,
                                 String draftStatus, String deliveredTo, String deliveryChannel,
                                 String deliveredBody, LocalDateTime deliveredAt) {
        this.enquiryId = enquiryId;
        this.enquiryCode = enquiryCode;
        this.draftId = draftId;
        this.enquiryStatus = enquiryStatus;
        this.draftStatus = draftStatus;
        this.deliveredTo = deliveredTo;
        this.deliveryChannel = deliveryChannel;
        this.deliveredBody = deliveredBody;
        this.deliveredAt = deliveredAt;
    }

    public Long getEnquiryId() {
        return enquiryId;
    }

    public void setEnquiryId(Long enquiryId) {
        this.enquiryId = enquiryId;
    }

    public String getEnquiryCode() {
        return enquiryCode;
    }

    public void setEnquiryCode(String enquiryCode) {
        this.enquiryCode = enquiryCode;
    }

    public Long getDraftId() {
        return draftId;
    }

    public void setDraftId(Long draftId) {
        this.draftId = draftId;
    }

    public String getEnquiryStatus() {
        return enquiryStatus;
    }

    public void setEnquiryStatus(String enquiryStatus) {
        this.enquiryStatus = enquiryStatus;
    }

    public String getDraftStatus() {
        return draftStatus;
    }

    public void setDraftStatus(String draftStatus) {
        this.draftStatus = draftStatus;
    }

    public String getDeliveredTo() {
        return deliveredTo;
    }

    public void setDeliveredTo(String deliveredTo) {
        this.deliveredTo = deliveredTo;
    }

    public String getDeliveryChannel() {
        return deliveryChannel;
    }

    public void setDeliveryChannel(String deliveryChannel) {
        this.deliveryChannel = deliveryChannel;
    }

    public String getDeliveredBody() {
        return deliveredBody;
    }

    public void setDeliveredBody(String deliveredBody) {
        this.deliveredBody = deliveredBody;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
