package com.companya.resort.dto;

import java.math.BigDecimal;
import java.util.List;

public class RoomCategoryAvailabilityDto {

    private Long id;
    private String code;
    private String name;
    private Integer totalPhysicalKeys;
    private Integer maxAdults;
    private Integer maxChildren;
    private String baseDescription;
    private List<String> photoUrls;
    private Boolean isBlackedOut;
    private String blackoutReason;
    private Boolean hasAllotment;
    private Integer remainingAllotment;
    private BigDecimal startingRatePerNight;
    private List<RatePlanPriceDto> ratePlans;

    public RoomCategoryAvailabilityDto() {
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

    public Integer getTotalPhysicalKeys() {
        return totalPhysicalKeys;
    }

    public void setTotalPhysicalKeys(Integer totalPhysicalKeys) {
        this.totalPhysicalKeys = totalPhysicalKeys;
    }

    public Integer getMaxAdults() {
        return maxAdults;
    }

    public void setMaxAdults(Integer maxAdults) {
        this.maxAdults = maxAdults;
    }

    public Integer getMaxChildren() {
        return maxChildren;
    }

    public void setMaxChildren(Integer maxChildren) {
        this.maxChildren = maxChildren;
    }

    public String getBaseDescription() {
        return baseDescription;
    }

    public void setBaseDescription(String baseDescription) {
        this.baseDescription = baseDescription;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }

    public Boolean getIsBlackedOut() {
        return isBlackedOut;
    }

    public void setIsBlackedOut(Boolean blackedOut) {
        isBlackedOut = blackedOut;
    }

    public String getBlackoutReason() {
        return blackoutReason;
    }

    public void setBlackoutReason(String blackoutReason) {
        this.blackoutReason = blackoutReason;
    }

    public Boolean getHasAllotment() {
        return hasAllotment;
    }

    public void setHasAllotment(Boolean hasAllotment) {
        this.hasAllotment = hasAllotment;
    }

    public Integer getRemainingAllotment() {
        return remainingAllotment;
    }

    public void setRemainingAllotment(Integer remainingAllotment) {
        this.remainingAllotment = remainingAllotment;
    }

    public BigDecimal getStartingRatePerNight() {
        return startingRatePerNight;
    }

    public void setStartingRatePerNight(BigDecimal startingRatePerNight) {
        this.startingRatePerNight = startingRatePerNight;
    }

    public List<RatePlanPriceDto> getRatePlans() {
        return ratePlans;
    }

    public void setRatePlans(List<RatePlanPriceDto> ratePlans) {
        this.ratePlans = ratePlans;
    }
}
