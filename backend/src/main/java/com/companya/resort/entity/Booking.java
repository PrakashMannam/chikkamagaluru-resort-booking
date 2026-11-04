package com.companya.resort.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_ref", nullable = false, unique = true, length = 12)
    private String bookingRef;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_category_id", nullable = false)
    private RoomCategory roomCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rate_plan_id", nullable = false)
    private RatePlan ratePlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id")
    private PromoCode promoCode;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(nullable = false)
    private Integer nights;

    @Column(name = "rooms_count", nullable = false)
    private Integer roomsCount = 1;

    @Column(nullable = false)
    private Integer adults;

    @Column(nullable = false)
    private Integer children = 0;

    @Column(name = "guest_name", nullable = false, length = 255)
    private String guestName;

    @Column(name = "guest_email", nullable = false, length = 255)
    private String guestEmail;

    @Column(name = "guest_phone", nullable = false, length = 50)
    private String guestPhone;

    @Column(name = "dpdp_consent_given", nullable = false)
    private Boolean dpdpConsentGiven = false;

    @Column(name = "dpdp_consent_timestamp")
    private LocalDateTime dpdpConsentTimestamp;

    @Column(name = "subtotal_before_discount_inr", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalBeforeDiscountInr;

    @Column(name = "discount_amount_inr", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmountInr = BigDecimal.ZERO;

    @Column(name = "taxable_amount_inr", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxableAmountInr;

    @Column(name = "gst_percentage_applied", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercentageApplied;

    @Column(name = "gst_amount_inr", nullable = false, precision = 10, scale = 2)
    private BigDecimal gstAmountInr;

    @Column(name = "total_payable_inr", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPayableInr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Booking() {
    }

    public Booking(Long id, String bookingRef, String idempotencyKey, RoomCategory roomCategory,
                   RatePlan ratePlan, PromoCode promoCode, LocalDate checkInDate, LocalDate checkOutDate,
                   Integer nights, Integer roomsCount, Integer adults, Integer children,
                   String guestName, String guestEmail, String guestPhone, Boolean dpdpConsentGiven,
                   LocalDateTime dpdpConsentTimestamp, BigDecimal subtotalBeforeDiscountInr,
                   BigDecimal discountAmountInr, BigDecimal taxableAmountInr,
                   BigDecimal gstPercentageApplied, BigDecimal gstAmountInr,
                   BigDecimal totalPayableInr, BookingStatus status, LocalDateTime expiresAt) {
        this.id = id;
        this.bookingRef = bookingRef;
        this.idempotencyKey = idempotencyKey;
        this.roomCategory = roomCategory;
        this.ratePlan = ratePlan;
        this.promoCode = promoCode;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.nights = nights;
        this.roomsCount = roomsCount;
        this.adults = adults;
        this.children = children;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestPhone = guestPhone;
        this.dpdpConsentGiven = dpdpConsentGiven;
        this.dpdpConsentTimestamp = dpdpConsentTimestamp;
        this.subtotalBeforeDiscountInr = subtotalBeforeDiscountInr;
        this.discountAmountInr = discountAmountInr;
        this.taxableAmountInr = taxableAmountInr;
        this.gstPercentageApplied = gstPercentageApplied;
        this.gstAmountInr = gstAmountInr;
        this.totalPayableInr = totalPayableInr;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingRef() {
        return bookingRef;
    }

    public void setBookingRef(String bookingRef) {
        this.bookingRef = bookingRef;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public RoomCategory getRoomCategory() {
        return roomCategory;
    }

    public void setRoomCategory(RoomCategory roomCategory) {
        this.roomCategory = roomCategory;
    }

    public RatePlan getRatePlan() {
        return ratePlan;
    }

    public void setRatePlan(RatePlan ratePlan) {
        this.ratePlan = ratePlan;
    }

    public PromoCode getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(PromoCode promoCode) {
        this.promoCode = promoCode;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public Integer getNights() {
        return nights;
    }

    public void setNights(Integer nights) {
        this.nights = nights;
    }

    public Integer getRoomsCount() {
        return roomsCount;
    }

    public void setRoomsCount(Integer roomsCount) {
        this.roomsCount = roomsCount;
    }

    public Integer getAdults() {
        return adults;
    }

    public void setAdults(Integer adults) {
        this.adults = adults;
    }

    public Integer getChildren() {
        return children;
    }

    public void setChildren(Integer children) {
        this.children = children;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public Boolean getDpdpConsentGiven() {
        return dpdpConsentGiven;
    }

    public void setDpdpConsentGiven(Boolean dpdpConsentGiven) {
        this.dpdpConsentGiven = dpdpConsentGiven;
    }

    public LocalDateTime getDpdpConsentTimestamp() {
        return dpdpConsentTimestamp;
    }

    public void setDpdpConsentTimestamp(LocalDateTime dpdpConsentTimestamp) {
        this.dpdpConsentTimestamp = dpdpConsentTimestamp;
    }

    public BigDecimal getSubtotalBeforeDiscountInr() {
        return subtotalBeforeDiscountInr;
    }

    public void setSubtotalBeforeDiscountInr(BigDecimal subtotalBeforeDiscountInr) {
        this.subtotalBeforeDiscountInr = subtotalBeforeDiscountInr;
    }

    public BigDecimal getDiscountAmountInr() {
        return discountAmountInr;
    }

    public void setDiscountAmountInr(BigDecimal discountAmountInr) {
        this.discountAmountInr = discountAmountInr;
    }

    public BigDecimal getTaxableAmountInr() {
        return taxableAmountInr;
    }

    public void setTaxableAmountInr(BigDecimal taxableAmountInr) {
        this.taxableAmountInr = taxableAmountInr;
    }

    public BigDecimal getGstPercentageApplied() {
        return gstPercentageApplied;
    }

    public void setGstPercentageApplied(BigDecimal gstPercentageApplied) {
        this.gstPercentageApplied = gstPercentageApplied;
    }

    public BigDecimal getGstAmountInr() {
        return gstAmountInr;
    }

    public void setGstAmountInr(BigDecimal gstAmountInr) {
        this.gstAmountInr = gstAmountInr;
    }

    public BigDecimal getTotalPayableInr() {
        return totalPayableInr;
    }

    public void setTotalPayableInr(BigDecimal totalPayableInr) {
        this.totalPayableInr = totalPayableInr;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
