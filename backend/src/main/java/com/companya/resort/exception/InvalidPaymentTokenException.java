package com.companya.resort.exception;

public class InvalidPaymentTokenException extends RuntimeException {
    public InvalidPaymentTokenException(String message) {
        super(message);
    }
}
