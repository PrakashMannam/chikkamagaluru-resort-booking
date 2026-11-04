package com.companya.resort.exception;

public class MaxActiveHoldsExceededException extends RuntimeException {
    public MaxActiveHoldsExceededException(String message) {
        super(message);
    }
}
