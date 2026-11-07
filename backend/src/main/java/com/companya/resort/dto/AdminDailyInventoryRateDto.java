package com.companya.resort.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminDailyInventoryRateDto(
        Long id,
        Long roomCategoryId,
        String categoryCode,
        String categoryName,
        LocalDate stayDate,
        BigDecimal baseRateInr,
        Integer websiteAllotmentTotal,
        Integer bookedCount,
        Integer totalPhysicalKeys,
        Long version,
        boolean blackout,
        String blackoutReason
) {
}
