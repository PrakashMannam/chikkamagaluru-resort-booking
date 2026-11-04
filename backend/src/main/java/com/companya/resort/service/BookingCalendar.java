package com.companya.resort.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class BookingCalendar {

    private final LocalDate fixedToday;
    private final Clock clock;

    public BookingCalendar(
            @Value("${app.booking.fixed-today:}") String fixedTodayStr,
            Clock clock
    ) {
        this.clock = clock;
        if (fixedTodayStr != null && !fixedTodayStr.trim().isEmpty()) {
            this.fixedToday = LocalDate.parse(fixedTodayStr.trim());
        } else {
            this.fixedToday = null;
        }
    }

    public LocalDate today() {
        if (fixedToday != null) {
            return fixedToday;
        }
        return LocalDate.now(clock);
    }
}
