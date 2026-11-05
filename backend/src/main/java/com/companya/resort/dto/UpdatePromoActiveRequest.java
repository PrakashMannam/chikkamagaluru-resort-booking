package com.companya.resort.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UpdatePromoActiveRequest(
        @JsonAlias({"active", "isActive", "is_active"})
        Boolean active
) {
    public boolean resolveActive() {
        return Boolean.TRUE.equals(active);
    }
}
