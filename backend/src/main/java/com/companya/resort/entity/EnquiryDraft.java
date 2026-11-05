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

import java.time.LocalDateTime;

@Entity
@Table(name = "enquiry_drafts")
public class EnquiryDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enquiry_id", nullable = false)
    private Enquiry enquiry;

    @Column(name = "draft_body", nullable = false, columnDefinition = "TEXT")
    private String draftBody;

    @Column(name = "quoted_rate_breakdown_json", columnDefinition = "JSON")
    private String quotedRateBreakdownJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false)
    private DraftGeneratedBy generatedBy = DraftGeneratedBy.AI_ROUTER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftStatus status = DraftStatus.DRAFT_PENDING_REVIEW;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sent_by_user_id")
    private User sentByUser;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public EnquiryDraft() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Enquiry getEnquiry() {
        return enquiry;
    }

    public void setEnquiry(Enquiry enquiry) {
        this.enquiry = enquiry;
    }

    public String getDraftBody() {
        return draftBody;
    }

    public void setDraftBody(String draftBody) {
        this.draftBody = draftBody;
    }

    public String getQuotedRateBreakdownJson() {
        return quotedRateBreakdownJson;
    }

    public void setQuotedRateBreakdownJson(String quotedRateBreakdownJson) {
        this.quotedRateBreakdownJson = quotedRateBreakdownJson;
    }

    public DraftGeneratedBy getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(DraftGeneratedBy generatedBy) {
        this.generatedBy = generatedBy;
    }

    public DraftStatus getStatus() {
        return status;
    }

    public void setStatus(DraftStatus status) {
        this.status = status;
    }

    public User getSentByUser() {
        return sentByUser;
    }

    public void setSentByUser(User sentByUser) {
        this.sentByUser = sentByUser;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
