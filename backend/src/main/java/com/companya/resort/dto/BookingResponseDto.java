package com.companya.resort.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookingResponseDto {

    private String bookingRef;
    private String idempotencyKey;
    private String status;
    private Long roomCategoryId;
    private String roomCategoryName;
    private Long ratePlanId;
    private String ratePlanName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private Integer roomsCount;
    private Integer adults;
    private Integer children;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private BigDecimal subtotalBeforeDiscountInr;
    private BigDecimal discountAmountInr;
    private BigDecimal taxableAmountInr;
    private BigDecimal gstPercentageApplied;
    private BigDecimal gstAmountInr;
    private BigDecimal totalPayableInr;
    private String gstVerificationStatus;
    private LocalDateTime expiresAt;
    private String oneTimePaymentToken;

    public BookingResponseDto() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getRoomCategoryId() {
        return roomCategoryId;
    }

    public void setRoomCategoryId(Long roomCategoryId) {
        this.roomCategoryId = roomCategoryId;
    }

    public String getRoomCategoryName() {
        return roomCategoryName;
    }

    public void setRoomCategoryName(String roomCategoryName) {
        this.roomCategoryName = roomCategoryName;
    }

    public Long getRatePlanId() {
        return ratePlanId;
    }

    public void setRatePlanId(Long ratePlanId) {
        this.ratePlanId = ratePlanId;
    }

    public String getRatePlanName() {
        return ratePlanName;
    }

    public void setRatePlanName(String ratePlanName) {
        this.ratePlanName = ratePlanName;
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

    public String getGstVerificationStatus() {
        return gstVerificationStatus;
    }

    public void setGstVerificationStatus(String gstVerificationStatus) {
        this.gstVerificationStatus = gstVerificationStatus;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getOneTimePaymentToken() {
        return oneTimePaymentToken;
    }

    public void setOneTimePaymentToken(String oneTimePaymentToken) {
        this.oneTimePaymentToken = oneTimePaymentToken;
    }
}
