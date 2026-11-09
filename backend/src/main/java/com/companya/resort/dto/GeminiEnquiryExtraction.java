package com.companya.resort.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiEnquiryExtraction {

    @JsonProperty("category")
    private String category;

    @JsonProperty("confidence")
    private BigDecimal confidence;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("check_in_date")
    private String checkInDate;

    @JsonProperty("check_out_date")
    private String checkOutDate;

    @JsonProperty("nights")
    private Integer nights;

    @JsonProperty("guests")
    private Integer guests;

    @JsonProperty("rooms")
    private Integer rooms;

    @JsonProperty("budget_amount")
    private BigDecimal budgetAmount;

    @JsonProperty("budget_currency")
    private String budgetCurrency;

    @JsonProperty("has_multiple_intents")
    private Boolean hasMultipleIntents;

    @JsonProperty("is_unsure")
    private Boolean isUnsure;

    @JsonProperty("accommodation_requirement")
    private String accommodationRequirement;

    @JsonProperty("suggested_reply")
    private String suggestedReply;

    public GeminiEnquiryExtraction() {
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }

    public String getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(String checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public Integer getNights() {
        return nights;
    }

    public void setNights(Integer nights) {
        this.nights = nights;
    }

    public Integer getGuests() {
        return guests;
    }

    public void setGuests(Integer guests) {
        this.guests = guests;
    }

    public Integer getRooms() {
        return rooms;
    }

    public void setRooms(Integer rooms) {
        this.rooms = rooms;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public String getBudgetCurrency() {
        return budgetCurrency;
    }

    public void setBudgetCurrency(String budgetCurrency) {
        this.budgetCurrency = budgetCurrency;
    }

    public Boolean getHasMultipleIntents() {
        return hasMultipleIntents;
    }

    public void setHasMultipleIntents(Boolean hasMultipleIntents) {
        this.hasMultipleIntents = hasMultipleIntents;
    }

    public Boolean getIsUnsure() {
        return isUnsure;
    }

    public void setIsUnsure(Boolean isUnsure) {
        this.isUnsure = isUnsure;
    }

    public String getAccommodationRequirement() {
        return accommodationRequirement;
    }

    public void setAccommodationRequirement(String accommodationRequirement) {
        this.accommodationRequirement = accommodationRequirement;
    }

    public String getSuggestedReply() {
        return suggestedReply;
    }

    public void setSuggestedReply(String suggestedReply) {
        this.suggestedReply = suggestedReply;
    }
}
