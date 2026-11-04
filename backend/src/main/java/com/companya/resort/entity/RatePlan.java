package com.companya.resort.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "rate_plans")
public class RatePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_category_id", nullable = false)
    private RoomCategory roomCategory;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_modifier_type", nullable = false, length = 32)
    private PriceModifierType priceModifierType;

    @Column(name = "price_modifier_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceModifierValue;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public RatePlan() {
    }

    public RatePlan(Long id, RoomCategory roomCategory, String code, String name,
                    PriceModifierType priceModifierType, BigDecimal priceModifierValue, Boolean isActive) {
        this.id = id;
        this.roomCategory = roomCategory;
        this.code = code;
        this.name = name;
        this.priceModifierType = priceModifierType;
        this.priceModifierValue = priceModifierValue;
        this.isActive = isActive;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RoomCategory getRoomCategory() {
        return roomCategory;
    }

    public void setRoomCategory(RoomCategory roomCategory) {
        this.roomCategory = roomCategory;
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

    public PriceModifierType getPriceModifierType() {
        return priceModifierType;
    }

    public void setPriceModifierType(PriceModifierType priceModifierType) {
        this.priceModifierType = priceModifierType;
    }

    public BigDecimal getPriceModifierValue() {
        return priceModifierValue;
    }

    public void setPriceModifierValue(BigDecimal priceModifierValue) {
        this.priceModifierValue = priceModifierValue;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
