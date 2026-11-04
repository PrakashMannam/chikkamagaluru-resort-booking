package com.companya.resort.dto;

import java.time.LocalDate;
import java.util.List;

public class AvailabilityResponse {

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private Integer rooms;
    private Integer adults;
    private Integer children;
    private List<RoomCategoryAvailabilityDto> categories;

    public AvailabilityResponse() {
    }

    public AvailabilityResponse(LocalDate checkInDate, LocalDate checkOutDate, Integer nights,
                                Integer rooms, Integer adults, Integer children,
                                List<RoomCategoryAvailabilityDto> categories) {
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.nights = nights;
        this.rooms = rooms;
        this.adults = adults;
        this.children = children;
        this.categories = categories;
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

    public Integer getRooms() {
        return rooms;
    }

    public void setRooms(Integer rooms) {
        this.rooms = rooms;
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

    public List<RoomCategoryAvailabilityDto> getCategories() {
        return categories;
    }

    public void setCategories(List<RoomCategoryAvailabilityDto> categories) {
        this.categories = categories;
    }
}
