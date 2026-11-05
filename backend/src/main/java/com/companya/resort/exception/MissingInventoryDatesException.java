package com.companya.resort.exception;

import java.time.LocalDate;
import java.util.List;

public class MissingInventoryDatesException extends RuntimeException {

    private final List<LocalDate> missingDates;

    public MissingInventoryDatesException(String message, List<LocalDate> missingDates) {
        super(message);
        this.missingDates = missingDates != null ? List.copyOf(missingDates) : List.of();
    }

    public List<LocalDate> getMissingDates() {
        return missingDates;
    }
}
