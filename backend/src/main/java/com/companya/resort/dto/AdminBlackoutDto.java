package com.companya.resort.dto;

import java.time.LocalDate;

public record AdminBlackoutDto(
        Long id,
        Long roomCategoryId,
        String roomCategoryCode,
        String roomCategoryName,
        LocalDate blackoutDate,
        String reason
) {
}
