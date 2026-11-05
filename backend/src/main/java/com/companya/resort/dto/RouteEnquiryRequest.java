package com.companya.resort.dto;

import com.companya.resort.entity.EnquiryCategory;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RouteEnquiryRequest {

    private EnquiryCategory category;

    @JsonProperty("assigned_to_user_id")
    @JsonAlias({"assigned_to_user_id", "assignedToUserId"})
    private Long assignedToUserId;

    private String note;

    public RouteEnquiryRequest() {
    }

    public RouteEnquiryRequest(EnquiryCategory category, Long assignedToUserId) {
        this.category = category;
        this.assignedToUserId = assignedToUserId;
    }

    public RouteEnquiryRequest(EnquiryCategory category, Long assignedToUserId, String note) {
        this.category = category;
        this.assignedToUserId = assignedToUserId;
        this.note = note;
    }

    public EnquiryCategory getCategory() {
        return category;
    }

    public void setCategory(EnquiryCategory category) {
        this.category = category;
    }

    public Long getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(Long assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
