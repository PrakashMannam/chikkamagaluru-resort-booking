package com.companya.resort.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "one_time_payment_token", nullable = false, unique = true, length = 64)
    private String oneTimePaymentToken;

    @Column(name = "token_used", nullable = false)
    private Boolean tokenUsed = false;

    @Column(name = "token_used_at")
    private LocalDateTime tokenUsedAt;

    @Column(name = "mock_gateway_ref", length = 255)
    private String mockGatewayRef;

    @Column(name = "amount_inr", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountInr;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public Payment() {
    }

    public Payment(Long id, Booking booking, String oneTimePaymentToken, Boolean tokenUsed,
                   LocalDateTime tokenUsedAt, String mockGatewayRef, BigDecimal amountInr,
                   String paymentMethod, PaymentStatus status, LocalDateTime paidAt) {
        this.id = id;
        this.booking = booking;
        this.oneTimePaymentToken = oneTimePaymentToken;
        this.tokenUsed = tokenUsed;
        this.tokenUsedAt = tokenUsedAt;
        this.mockGatewayRef = mockGatewayRef;
        this.amountInr = amountInr;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.paidAt = paidAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public String getOneTimePaymentToken() {
        return oneTimePaymentToken;
    }

    public void setOneTimePaymentToken(String oneTimePaymentToken) {
        this.oneTimePaymentToken = oneTimePaymentToken;
    }

    public Boolean getTokenUsed() {
        return tokenUsed;
    }

    public void setTokenUsed(Boolean tokenUsed) {
        this.tokenUsed = tokenUsed;
    }

    public LocalDateTime getTokenUsedAt() {
        return tokenUsedAt;
    }

    public void setTokenUsedAt(LocalDateTime tokenUsedAt) {
        this.tokenUsedAt = tokenUsedAt;
    }

    public String getMockGatewayRef() {
        return mockGatewayRef;
    }

    public void setMockGatewayRef(String mockGatewayRef) {
        this.mockGatewayRef = mockGatewayRef;
    }

    public BigDecimal getAmountInr() {
        return amountInr;
    }

    public void setAmountInr(BigDecimal amountInr) {
        this.amountInr = amountInr;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
