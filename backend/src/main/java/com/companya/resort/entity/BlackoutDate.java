package com.companya.resort.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "blackout_dates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_blackout_dates_cat_date", columnNames = {"room_category_id", "blackout_date"})
})
public class BlackoutDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_category_id")
    private RoomCategory roomCategory;

    @Column(name = "blackout_date", nullable = false)
    private LocalDate blackoutDate;

    @Column(length = 255)
    private String reason;

    public BlackoutDate() {
    }

    public BlackoutDate(Long id, RoomCategory roomCategory, LocalDate blackoutDate, String reason) {
        this.id = id;
        this.roomCategory = roomCategory;
        this.blackoutDate = blackoutDate;
        this.reason = reason;
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

    public LocalDate getBlackoutDate() {
        return blackoutDate;
    }

    public void setBlackoutDate(LocalDate blackoutDate) {
        this.blackoutDate = blackoutDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
