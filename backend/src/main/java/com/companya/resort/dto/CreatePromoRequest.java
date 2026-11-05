package com.companya.resort.dto;

import com.companya.resort.entity.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePromoRequest(
        @NotBlank(message = "Promo code is required")
        String code,

        @NotNull(message = "Discount type is required")
        DiscountType discountType,

        @NotNull(message = "Discount value is required")
        BigDecimal discountValue,

        @NotNull(message = "validFrom date is required")
        LocalDate validFrom,

        @NotNull(message = "validTo date is required")
        LocalDate validTo,

        Integer minNights,

        @NotNull(message = "maxUses is required")
        Integer maxUses,

        Boolean isActive
) {
}
