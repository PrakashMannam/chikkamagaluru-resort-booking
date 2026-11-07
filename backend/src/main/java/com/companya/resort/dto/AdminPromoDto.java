package com.companya.resort.dto;

import com.companya.resort.entity.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminPromoDto(
        Long id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        LocalDate validFrom,
        LocalDate validTo,
        Integer minNights,
        Integer maxUses,
        Integer currentUses,
        Boolean isActive
) {
}
