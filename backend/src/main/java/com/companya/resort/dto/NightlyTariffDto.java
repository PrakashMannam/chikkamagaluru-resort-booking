package com.companya.resort.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NightlyTariffDto {

    private LocalDate stayDate;
    private BigDecimal baseRateInr;
    private BigDecimal ratePlanAddonInr;
    private BigDecimal nightlyTotalInr;

    public NightlyTariffDto() {
    }

    public NightlyTariffDto(LocalDate stayDate, BigDecimal baseRateInr, BigDecimal ratePlanAddonInr, BigDecimal nightlyTotalInr) {
        this.stayDate = stayDate;
        this.baseRateInr = baseRateInr;
        this.ratePlanAddonInr = ratePlanAddonInr;
        this.nightlyTotalInr = nightlyTotalInr;
    }

    public LocalDate getStayDate() {
        return stayDate;
    }

    public void setStayDate(LocalDate stayDate) {
        this.stayDate = stayDate;
    }

    public BigDecimal getBaseRateInr() {
        return baseRateInr;
    }

    public void setBaseRateInr(BigDecimal baseRateInr) {
        this.baseRateInr = baseRateInr;
    }

    public BigDecimal getRatePlanAddonInr() {
        return ratePlanAddonInr;
    }

    public void setRatePlanAddonInr(BigDecimal ratePlanAddonInr) {
        this.ratePlanAddonInr = ratePlanAddonInr;
    }

    public BigDecimal getNightlyTotalInr() {
        return nightlyTotalInr;
    }

    public void setNightlyTotalInr(BigDecimal nightlyTotalInr) {
        this.nightlyTotalInr = nightlyTotalInr;
    }
}
