package com.companya.resort.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PricingQuoteDto {

    private Long roomCategoryId;
    private String roomCategoryCode;
    private String roomCategoryName;
    private Long ratePlanId;
    private String ratePlanCode;
    private String ratePlanName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private Integer roomsCount;
    private Integer adults;
    private Integer children;

    // Itemized server pricing
    private BigDecimal baseAccommodationTotalInr;
    private BigDecimal ratePlanAddonTotalInr;
    private BigDecimal subtotalBeforeDiscountInr;
    private String promoCode;
    private BigDecimal discountAmountInr;
    private BigDecimal discountedPreTaxInr;
    private BigDecimal taxableAmountInr;
    private BigDecimal gstPercentageApplied;
    private BigDecimal gstAmountInr;
    private BigDecimal totalPayableInr;
    private String gstVerificationStatus;

    // Nightly itemization
    private List<NightlyTariffDto> nightlyRates = new ArrayList<>();

    public PricingQuoteDto() {
    }

    public Long getRoomCategoryId() {
        return roomCategoryId;
    }

    public void setRoomCategoryId(Long roomCategoryId) {
        this.roomCategoryId = roomCategoryId;
    }

    public String getRoomCategoryCode() {
        return roomCategoryCode;
    }

    public void setRoomCategoryCode(String roomCategoryCode) {
        this.roomCategoryCode = roomCategoryCode;
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

    public String getRatePlanCode() {
        return ratePlanCode;
    }

    public void setRatePlanCode(String ratePlanCode) {
        this.ratePlanCode = ratePlanCode;
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

    public BigDecimal getBaseAccommodationTotalInr() {
        return baseAccommodationTotalInr;
    }

    public void setBaseAccommodationTotalInr(BigDecimal baseAccommodationTotalInr) {
        this.baseAccommodationTotalInr = baseAccommodationTotalInr;
    }

    public BigDecimal getRatePlanAddonTotalInr() {
        return ratePlanAddonTotalInr;
    }

    public void setRatePlanAddonTotalInr(BigDecimal ratePlanAddonTotalInr) {
        this.ratePlanAddonTotalInr = ratePlanAddonTotalInr;
    }

    public BigDecimal getSubtotalBeforeDiscountInr() {
        return subtotalBeforeDiscountInr;
    }

    public void setSubtotalBeforeDiscountInr(BigDecimal subtotalBeforeDiscountInr) {
        this.subtotalBeforeDiscountInr = subtotalBeforeDiscountInr;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public BigDecimal getDiscountAmountInr() {
        return discountAmountInr;
    }

    public void setDiscountAmountInr(BigDecimal discountAmountInr) {
        this.discountAmountInr = discountAmountInr;
    }

    public BigDecimal getDiscountedPreTaxInr() {
        return discountedPreTaxInr;
    }

    public void setDiscountedPreTaxInr(BigDecimal discountedPreTaxInr) {
        this.discountedPreTaxInr = discountedPreTaxInr;
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

    public List<NightlyTariffDto> getNightlyRates() {
        return nightlyRates;
    }

    public void setNightlyRates(List<NightlyTariffDto> nightlyRates) {
        this.nightlyRates = nightlyRates;
    }
}
