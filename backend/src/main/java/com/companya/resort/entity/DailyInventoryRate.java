package com.companya.resort.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_inventory_rates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_inventory_rates_cat_date", columnNames = {"room_category_id", "stay_date"})
})
public class DailyInventoryRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_category_id", nullable = false)
    private RoomCategory roomCategory;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    @Column(name = "base_rate_inr", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRateInr;

    @Column(name = "website_allotment_total", nullable = false)
    private Integer websiteAllotmentTotal;

    @Column(name = "booked_count", nullable = false)
    private Integer bookedCount = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public DailyInventoryRate() {
    }

    public DailyInventoryRate(Long id, RoomCategory roomCategory, LocalDate stayDate,
                              BigDecimal baseRateInr, Integer websiteAllotmentTotal,
                              Integer bookedCount, Long version) {
        this.id = id;
        this.roomCategory = roomCategory;
        this.stayDate = stayDate;
        this.baseRateInr = baseRateInr;
        this.websiteAllotmentTotal = websiteAllotmentTotal;
        this.bookedCount = bookedCount;
        this.version = version;
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

    public Integer getWebsiteAllotmentTotal() {
        return websiteAllotmentTotal;
    }

    public void setWebsiteAllotmentTotal(Integer websiteAllotmentTotal) {
        this.websiteAllotmentTotal = websiteAllotmentTotal;
    }

    public Integer getBookedCount() {
        return bookedCount;
    }

    public void setBookedCount(Integer bookedCount) {
        this.bookedCount = bookedCount;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
