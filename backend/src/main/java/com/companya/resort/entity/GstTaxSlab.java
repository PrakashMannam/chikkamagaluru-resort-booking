package com.companya.resort.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gst_tax_slabs")
public class GstTaxSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "min_tariff_inr", nullable = false, precision = 10, scale = 2)
    private BigDecimal minTariffInr;

    @Column(name = "max_tariff_inr", precision = 10, scale = 2)
    private BigDecimal maxTariffInr;

    @Column(name = "gst_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercentage;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 32)
    private GstVerificationStatus verificationStatus = GstVerificationStatus.UNVERIFIED;

    public GstTaxSlab() {
    }

    public GstTaxSlab(Long id, BigDecimal minTariffInr, BigDecimal maxTariffInr,
                      BigDecimal gstPercentage, LocalDate effectiveFrom, LocalDate effectiveTo,
                      GstVerificationStatus verificationStatus) {
        this.id = id;
        this.minTariffInr = minTariffInr;
        this.maxTariffInr = maxTariffInr;
        this.gstPercentage = gstPercentage;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.verificationStatus = verificationStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getMinTariffInr() {
        return minTariffInr;
    }

    public void setMinTariffInr(BigDecimal minTariffInr) {
        this.minTariffInr = minTariffInr;
    }

    public BigDecimal getMaxTariffInr() {
        return maxTariffInr;
    }

    public void setMaxTariffInr(BigDecimal maxTariffInr) {
        this.maxTariffInr = maxTariffInr;
    }

    public BigDecimal getGstPercentage() {
        return gstPercentage;
    }

    public void setGstPercentage(BigDecimal gstPercentage) {
        this.gstPercentage = gstPercentage;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public GstVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(GstVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}
