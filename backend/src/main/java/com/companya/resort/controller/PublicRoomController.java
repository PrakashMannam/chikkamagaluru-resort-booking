package com.companya.resort.controller;

import com.companya.resort.dto.AvailabilityResponse;
import com.companya.resort.dto.BookingResponseDto;
import com.companya.resort.dto.CancelHoldRequest;
import com.companya.resort.dto.CreateBookingRequest;
import com.companya.resort.dto.PaymentWebhookRequest;
import com.companya.resort.dto.PricingQuoteDto;
import com.companya.resort.service.BookingPaymentService;
import com.companya.resort.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/public")
public class PublicRoomController {

    private final BookingService bookingService;
    private final BookingPaymentService bookingPaymentService;

    public PublicRoomController(
            BookingService bookingService,
            @Autowired(required = false) BookingPaymentService bookingPaymentService
    ) {
        this.bookingService = bookingService;
        this.bookingPaymentService = bookingPaymentService;
    }

    @GetMapping("/rooms/availability")
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int rooms,
            @RequestParam(defaultValue = "2") int adults,
            @RequestParam(defaultValue = "0") int children
    ) {
        AvailabilityResponse response = bookingService.checkAvailability(checkIn, checkOut, rooms, adults, children);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/pricing/quote", "/rooms/quote"})
    public ResponseEntity<PricingQuoteDto> getPricingQuote(
            @RequestParam(required = false) Long roomCategoryId,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) Long ratePlanId,
            @RequestParam(required = false) String planCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int rooms,
            @RequestParam(defaultValue = "2") int adults,
            @RequestParam(defaultValue = "0") int children,
            @RequestParam(required = false) String promoCode
    ) {
        PricingQuoteDto quote = bookingService.calculatePricingQuote(
                roomCategoryId, categoryCode, ratePlanId, planCode,
                checkIn, checkOut, rooms, adults, children, promoCode
        );
        return ResponseEntity.ok(quote);
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponseDto> createBooking(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        BookingResponseDto response = bookingService.createBookingAndLockAllotment(request, idempotencyKeyHeader);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments/{bookingRef}/mock-webhook")
    public ResponseEntity<BookingResponseDto> mockPaymentWebhook(
            @PathVariable String bookingRef,
            @RequestHeader(value = "X-Payment-Token", required = false) String tokenHeader,
            @RequestBody(required = false) PaymentWebhookRequest request
    ) {
        String token = (request != null && request.getEffectiveToken() != null && !request.getEffectiveToken().isBlank())
                ? request.getEffectiveToken()
                : tokenHeader;

        BookingResponseDto response = bookingPaymentService.processMockWebhook(bookingRef, token, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bookings/{bookingRef}/cancel-hold")
    public ResponseEntity<BookingResponseDto> cancelHold(
            @PathVariable String bookingRef,
            @RequestHeader(value = "X-Payment-Token", required = false) String tokenHeader,
            @RequestParam(value = "token", required = false) String tokenParam,
            @RequestBody(required = false) CancelHoldRequest request
    ) {
        String token = null;
        if (request != null && request.getToken() != null && !request.getToken().isBlank()) {
            token = request.getToken();
        } else if (tokenHeader != null && !tokenHeader.isBlank()) {
            token = tokenHeader;
        } else if (tokenParam != null && !tokenParam.isBlank()) {
            token = tokenParam;
        }

        BookingResponseDto response = bookingPaymentService.cancelHold(bookingRef, token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings/{bookingRef}")
    public ResponseEntity<BookingResponseDto> getBooking(@PathVariable String bookingRef) {
        BookingResponseDto response = bookingPaymentService.getBookingMasked(bookingRef);
        return ResponseEntity.ok(response);
    }
}
