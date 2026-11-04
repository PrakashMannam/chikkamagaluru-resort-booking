package com.companya.resort.exception;

public class IdempotencyPayloadMismatchException extends RuntimeException {
    public IdempotencyPayloadMismatchException(String message) {
        super(message);
    }
}
