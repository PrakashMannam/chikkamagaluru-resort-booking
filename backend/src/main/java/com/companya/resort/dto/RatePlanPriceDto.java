package com.companya.resort.dto;

import java.math.BigDecimal;

public class RatePlanPriceDto {

    private Long id;
    private String code;
    private String name;
    private String priceModifierType;
    private BigDecimal priceModifierValue;
    private BigDecimal subtotalBeforeDiscountInr;
    private BigDecimal taxableAmountInr;
    private BigDecimal gstPercentageApplied;
    private BigDecimal gstAmountInr;
    private BigDecimal totalPayableInr;
    private String gstVerificationStatus;

    public RatePlanPriceDto() {
    }

    public RatePlanPriceDto(Long id, String code, String name, String priceModifierType,
                            BigDecimal priceModifierValue, BigDecimal subtotalBeforeDiscountInr,
                            BigDecimal taxableAmountInr, BigDecimal gstPercentageApplied,
                            BigDecimal gstAmountInr, BigDecimal totalPayableInr,
                            String gstVerificationStatus) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.priceModifierType = priceModifierType;
        this.priceModifierValue = priceModifierValue;
        this.subtotalBeforeDiscountInr = subtotalBeforeDiscountInr;
        this.taxableAmountInr = taxableAmountInr;
        this.gstPercentageApplied = gstPercentageApplied;
        this.gstAmountInr = gstAmountInr;
        this.totalPayableInr = totalPayableInr;
        this.gstVerificationStatus = gstVerificationStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPriceModifierType() {
        return priceModifierType;
    }

    public void setPriceModifierType(String priceModifierType) {
        this.priceModifierType = priceModifierType;
    }

    public BigDecimal getPriceModifierValue() {
        return priceModifierValue;
    }

    public void setPriceModifierValue(BigDecimal priceModifierValue) {
        this.priceModifierValue = priceModifierValue;
    }

    public BigDecimal getSubtotalBeforeDiscountInr() {
        return subtotalBeforeDiscountInr;
    }

    public void setSubtotalBeforeDiscountInr(BigDecimal subtotalBeforeDiscountInr) {
        this.subtotalBeforeDiscountInr = subtotalBeforeDiscountInr;
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
}
