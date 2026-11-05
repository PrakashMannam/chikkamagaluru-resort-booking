package com.companya.resort.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "enquiries")
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enquiry_code", nullable = false, unique = true, length = 50)
    private String enquiryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_channel", nullable = false)
    private EnquiryChannel sourceChannel;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(name = "sender_contact", nullable = false)
    private String senderContact;

    @Column(name = "raw_message", nullable = false, columnDefinition = "TEXT")
    private String rawMessage;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnquiryCategory category;

    @Column(name = "ai_confidence", precision = 4, scale = 3)
    private BigDecimal aiConfidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "routing_method", nullable = false)
    private RoutingMethod routingMethod;

    @Column(name = "extracted_check_in")
    private LocalDate extractedCheckIn;

    @Column(name = "extracted_check_out")
    private LocalDate extractedCheckOut;

    @Column(name = "extracted_nights")
    private Integer extractedNights;

    @Column(name = "extracted_guests")
    private Integer extractedGuests;

    @Column(name = "extracted_rooms")
    private Integer extractedRooms;

    @Column(name = "extracted_budget_inr", precision = 12, scale = 2)
    private BigDecimal extractedBudgetInr;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedToUser;

    @Column(name = "gm_alerted", nullable = false)
    private Boolean gmAlerted = false;

    @Column(name = "ai_error_status", length = 100)
    private String aiErrorStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnquiryStatus status = EnquiryStatus.OPEN;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "first_replied_at")
    private LocalDateTime firstRepliedAt;

    public Enquiry() {
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

    public EnquiryChannel getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(EnquiryChannel sourceChannel) {
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

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public EnquiryCategory getCategory() {
        return category;
    }

    public void setCategory(EnquiryCategory category) {
        this.category = category;
    }

    public BigDecimal getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(BigDecimal aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public RoutingMethod getRoutingMethod() {
        return routingMethod;
    }

    public void setRoutingMethod(RoutingMethod routingMethod) {
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

    public User getAssignedToUser() {
        return assignedToUser;
    }

    public void setAssignedToUser(User assignedToUser) {
        this.assignedToUser = assignedToUser;
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

    public EnquiryStatus getStatus() {
        return status;
    }

    public void setStatus(EnquiryStatus status) {
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
}
