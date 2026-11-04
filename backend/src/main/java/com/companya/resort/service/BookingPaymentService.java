package com.companya.resort.service;

import com.companya.resort.dto.BookingResponseDto;
import com.companya.resort.dto.PaymentWebhookRequest;
import com.companya.resort.entity.Booking;
import com.companya.resort.entity.BookingStatus;
import com.companya.resort.entity.DailyInventoryRate;
import com.companya.resort.entity.Payment;
import com.companya.resort.entity.PaymentStatus;
import com.companya.resort.exception.BookingHoldExpiredException;
import com.companya.resort.exception.InvalidPaymentTokenException;
import com.companya.resort.exception.PaymentTokenAlreadyUsedException;
import com.companya.resort.exception.ResourceNotFoundException;
import com.companya.resort.repository.BookingRepository;
import com.companya.resort.repository.DailyInventoryRateRepository;
import com.companya.resort.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingPaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final DailyInventoryRateRepository dailyInventoryRateRepository;
    private final BookingTransactionService bookingTransactionService;
    private final Clock clock;

    public BookingPaymentService(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            DailyInventoryRateRepository dailyInventoryRateRepository,
            BookingTransactionService bookingTransactionService,
            Clock clock
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.dailyInventoryRateRepository = dailyInventoryRateRepository;
        this.bookingTransactionService = bookingTransactionService;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponseDto processMockWebhook(String bookingRef, String providedToken, PaymentWebhookRequest request) {
        if (providedToken == null || providedToken.trim().isEmpty()) {
            throw new IllegalArgumentException("One-time payment token is required");
        }
        String cleanToken = providedToken.trim();

        // 1. Lock the booking row FIRST
        Booking booking = bookingRepository.findAndLockByBookingRef(bookingRef)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + bookingRef));

        // 2. Fetch payment row
        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for booking: " + bookingRef));

        // 3. Verify token match
        if (!cleanToken.equals(payment.getOneTimePaymentToken())) {
            throw new InvalidPaymentTokenException("Invalid payment token");
        }

        // 4. Token is single use
        if (Boolean.TRUE.equals(payment.getTokenUsed())) {
            throw new PaymentTokenAlreadyUsedException("Payment token has already been used");
        }

        // 5. Re-check status and expiry
        LocalDateTime now = LocalDateTime.now(clock);
        if (booking.getStatus() == BookingStatus.EXPIRED || !booking.getExpiresAt().isAfter(now)) {
            booking.setStatus(BookingStatus.EXPIRED);
            payment.setTokenUsed(true);
            payment.setTokenUsedAt(now);
            bookingRepository.save(booking);
            paymentRepository.save(payment);
            throw new BookingHoldExpiredException("Booking hold has expired; late payment rejected");
        }

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Booking is not in PENDING_PAYMENT status: " + booking.getStatus());
        }

        // 6. Token is single use: mark used
        payment.setTokenUsed(true);
        payment.setTokenUsedAt(now);
        if (request != null) {
            if (request.getMockGatewayRef() != null) {
                payment.setMockGatewayRef(request.getMockGatewayRef());
            }
            if (request.getPaymentMethod() != null) {
                payment.setPaymentMethod(request.getPaymentMethod());
            }
        }

        // 7. SUCCESS -> CONFIRMED. FAILURE -> FAILED and release stock.
        String statusStr = (request != null && request.getStatus() != null) ? request.getStatus().trim().toUpperCase() : "SUCCESS";
        if ("SUCCESS".equals(statusStr)) {
            booking.setStatus(BookingStatus.CONFIRMED);
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(now);
            bookingRepository.save(booking);
            paymentRepository.save(payment);
        } else {
            booking.setStatus(BookingStatus.FAILED);
            payment.setStatus(PaymentStatus.FAILURE);
            bookingRepository.save(booking);
            paymentRepository.save(payment);

            // Lock inventory rows in ascending date order and release stock
            List<DailyInventoryRate> lockedRates = dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(
                    booking.getRoomCategory().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate()
            );
            for (DailyInventoryRate rate : lockedRates) {
                rate.setBookedCount(Math.max(0, rate.getBookedCount() - booking.getRoomsCount()));
                dailyInventoryRateRepository.save(rate);
            }
        }

        return bookingTransactionService.mapToBookingResponseDto(booking, payment.getOneTimePaymentToken());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponseDto cancelHold(String bookingRef, String providedToken) {
        if (providedToken == null || providedToken.trim().isEmpty()) {
            throw new IllegalArgumentException("One-time payment token is required to cancel hold");
        }
        String cleanToken = providedToken.trim();

        // 1. Lock the booking row FIRST
        Booking booking = bookingRepository.findAndLockByBookingRef(bookingRef)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + bookingRef));

        // 2. Fetch payment row
        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for booking: " + bookingRef));

        // 3. Verify token match
        if (!cleanToken.equals(payment.getOneTimePaymentToken())) {
            throw new InvalidPaymentTokenException("Invalid payment token");
        }

        // 4. Check status
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only bookings in PENDING_PAYMENT status can be cancelled, current status: " + booking.getStatus());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        booking.setStatus(BookingStatus.CANCELLED);
        payment.setTokenUsed(true);
        payment.setTokenUsedAt(now);
        bookingRepository.save(booking);
        paymentRepository.save(payment);

        // 5. Lock inventory rows in ascending date order and release stock
        List<DailyInventoryRate> lockedRates = dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(
                booking.getRoomCategory().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );
        for (DailyInventoryRate rate : lockedRates) {
            rate.setBookedCount(Math.max(0, rate.getBookedCount() - booking.getRoomsCount()));
            dailyInventoryRateRepository.save(rate);
        }

        return bookingTransactionService.mapToBookingResponseDto(booking, payment.getOneTimePaymentToken());
    }

    public BookingResponseDto getBookingMasked(String bookingRef) {
        Booking booking = bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + bookingRef));

        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        String token = (payment != null && !Boolean.TRUE.equals(payment.getTokenUsed())) ? payment.getOneTimePaymentToken() : null;

        BookingResponseDto dto = bookingTransactionService.mapToBookingResponseDto(booking, token);
        dto.setGuestEmail(maskEmail(booking.getGuestEmail()));
        dto.setGuestPhone(maskPhone(booking.getGuestPhone()));
        return dto;
    }

    public String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String prefix = email.substring(0, 1);
        String domain = email.substring(atIndex);
        return prefix + "***" + domain;
    }

    public String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String clean = phone.trim();
        if (clean.length() < 4) {
            return "****";
        }
        String first2 = clean.substring(0, 2);
        String last2 = clean.substring(clean.length() - 2);
        int middleLen = clean.length() - 4;
        String maskedMiddle = "X".repeat(Math.max(middleLen, 6));
        return first2 + maskedMiddle + last2;
    }
}
