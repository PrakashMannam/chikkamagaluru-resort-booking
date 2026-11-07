package com.companya.resort.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateBlackoutRequest(
        String roomCategoryCode,
        Long roomCategoryId,
        @NotNull(message = "Blackout date is required")
        LocalDate blackoutDate,
        String reason
) {
}
