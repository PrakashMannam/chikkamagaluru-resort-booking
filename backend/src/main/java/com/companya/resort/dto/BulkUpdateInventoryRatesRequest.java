package com.companya.resort.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BulkUpdateInventoryRatesRequest(
        @NotBlank(message = "Room category code is required")
        String roomCategoryCode,

        @NotNull(message = "'from' date is required")
        LocalDate from,

        @NotNull(message = "'to' date is required")
        LocalDate to,

        @NotNull(message = "Base rate is required")
        BigDecimal baseRateInr,

        @NotNull(message = "Website allotment total is required")
        Integer websiteAllotmentTotal
) {
}
