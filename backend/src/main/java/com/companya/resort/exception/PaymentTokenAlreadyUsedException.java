package com.companya.resort.exception;

public class PaymentTokenAlreadyUsedException extends RuntimeException {
    public PaymentTokenAlreadyUsedException(String message) {
        super(message);
    }
}
