package com.companya.resort.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDateRange(InvalidDateRangeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Date Range", ex.getMessage());
    }

    @ExceptionHandler(MissingInventoryDatesException.class)
    public ResponseEntity<Map<String, Object>> handleMissingInventoryDates(MissingInventoryDatesException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Unprocessable Entity");
        body.put("message", ex.getMessage());
        body.put("missingDates", ex.getMissingDates());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(InvalidPromoCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPromoCode(InvalidPromoCodeException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Promo Code", ex.getMessage());
    }

    @ExceptionHandler(IdempotencyPayloadMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleIdempotencyPayloadMismatch(IdempotencyPayloadMismatchException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency Payload Mismatch", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage());
    }

    @ExceptionHandler(BlackoutDateConflictException.class)
    public ResponseEntity<Map<String, Object>> handleBlackoutConflict(BlackoutDateConflictException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Blackout Date Conflict", ex.getMessage());
    }

    @ExceptionHandler(InsufficientAllotmentException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientAllotment(InsufficientAllotmentException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Insufficient Allotment", ex.getMessage());
    }

    @ExceptionHandler(MaxActiveHoldsExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxActiveHolds(MaxActiveHoldsExceededException ex) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "Active Hold Cap Exceeded", ex.getMessage());
    }

    @ExceptionHandler(BookingHoldExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleBookingHoldExpired(BookingHoldExpiredException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Booking Hold Expired", ex.getMessage());
    }

    @ExceptionHandler(PaymentTokenAlreadyUsedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentTokenAlreadyUsed(PaymentTokenAlreadyUsedException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Payment Token Already Used", ex.getMessage());
    }

    @ExceptionHandler(InvalidPaymentTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPaymentToken(InvalidPaymentTokenException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Payment Token", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(ServiceUnavailableException ex) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Validation Failed");
        body.put("message", "Request validation failed on one or more fields");
        body.put("details", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden",
                ex.getMessage() != null ? ex.getMessage() : "Access denied: insufficient permissions");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
