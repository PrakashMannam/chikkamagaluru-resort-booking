package com.companya.resort.dto;

import com.companya.resort.entity.PromoCode;
import java.math.BigDecimal;

public class PriceCalculationResult {

    private BigDecimal subtotalBeforeDiscountInr;
    private BigDecimal discountAmountInr;
    private BigDecimal taxableAmountInr;
    private BigDecimal gstPercentageApplied;
    private BigDecimal gstAmountInr;
    private BigDecimal totalPayableInr;
    private String gstVerificationStatus;
    private PromoCode promoCode;

    public PriceCalculationResult() {
    }

    public PriceCalculationResult(BigDecimal subtotalBeforeDiscountInr, BigDecimal discountAmountInr,
                                  BigDecimal taxableAmountInr, BigDecimal gstPercentageApplied,
                                  BigDecimal gstAmountInr, BigDecimal totalPayableInr,
                                  String gstVerificationStatus, PromoCode promoCode) {
        this.subtotalBeforeDiscountInr = subtotalBeforeDiscountInr;
        this.discountAmountInr = discountAmountInr;
        this.taxableAmountInr = taxableAmountInr;
        this.gstPercentageApplied = gstPercentageApplied;
        this.gstAmountInr = gstAmountInr;
        this.totalPayableInr = totalPayableInr;
        this.gstVerificationStatus = gstVerificationStatus;
        this.promoCode = promoCode;
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

    public PromoCode getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(PromoCode promoCode) {
        this.promoCode = promoCode;
    }
}
