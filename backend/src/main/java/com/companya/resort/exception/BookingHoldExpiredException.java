package com.companya.resort.exception;

public class BookingHoldExpiredException extends RuntimeException {
    public BookingHoldExpiredException(String message) {
        super(message);
    }
}
