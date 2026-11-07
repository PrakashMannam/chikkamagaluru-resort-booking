package com.companya.resort.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public class CreateBookingRequest {

    private String idempotencyKey;

    @NotNull(message = "Room category ID is required")
    private Long roomCategoryId;

    @NotNull(message = "Rate plan ID is required")
    private Long ratePlanId;

    @NotNull(message = "Check-in date is required")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    private LocalDate checkOutDate;

    @Min(value = 1, message = "At least 1 room is required")
    private Integer roomsCount = 1;

    @NotNull(message = "Number of adults is required")
    @Min(value = 1, message = "At least 1 adult is required")
    private Integer adults = 1;

    @Min(value = 0, message = "Children count cannot be negative")
    private Integer children = 0;

    @NotBlank(message = "Guest name is required")
    private String guestName;

    @NotBlank(message = "Guest email is required")
    @Email(message = "Invalid guest email format")
    private String guestEmail;

    @NotBlank(message = "Guest phone is required")
    @Pattern(
            regexp = "^(?:\\+?91[\\-\\s]?)?[0]?([6-9])(?!\\1{9})\\d{9}$",
            message = "Please enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9"
    )
    private String guestPhone;

    private String promoCode;

    @NotNull(message = "DPDP consent is mandatory")
    private Boolean dpdpConsentGiven;

    public CreateBookingRequest() {
    }

    public CreateBookingRequest(String idempotencyKey, Long roomCategoryId, Long ratePlanId,
                                LocalDate checkInDate, LocalDate checkOutDate, Integer roomsCount,
                                Integer adults, Integer children, String guestName,
                                String guestEmail, String guestPhone, String promoCode,
                                Boolean dpdpConsentGiven) {
        this.idempotencyKey = idempotencyKey;
        this.roomCategoryId = roomCategoryId;
        this.ratePlanId = ratePlanId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.roomsCount = roomsCount != null ? roomsCount : 1;
        this.adults = adults != null ? adults : 1;
        this.children = children != null ? children : 0;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestPhone = guestPhone;
        this.promoCode = promoCode;
        this.dpdpConsentGiven = dpdpConsentGiven;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getRoomCategoryId() {
        return roomCategoryId;
    }

    public void setRoomCategoryId(Long roomCategoryId) {
        this.roomCategoryId = roomCategoryId;
    }

    public Long getRatePlanId() {
        return ratePlanId;
    }

    public void setRatePlanId(Long ratePlanId) {
        this.ratePlanId = ratePlanId;
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

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public Boolean getDpdpConsentGiven() {
        return dpdpConsentGiven;
    }

    public void setDpdpConsentGiven(Boolean dpdpConsentGiven) {
        this.dpdpConsentGiven = dpdpConsentGiven;
    }
}
