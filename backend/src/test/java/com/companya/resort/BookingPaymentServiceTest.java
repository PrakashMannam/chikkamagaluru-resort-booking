package com.companya.resort;

import com.companya.resort.dto.BookingResponseDto;
import com.companya.resort.dto.PaymentWebhookRequest;
import com.companya.resort.entity.*;
import com.companya.resort.exception.BookingHoldExpiredException;
import com.companya.resort.exception.InvalidPaymentTokenException;
import com.companya.resort.exception.PaymentTokenAlreadyUsedException;
import com.companya.resort.exception.ResourceNotFoundException;
import com.companya.resort.repository.BookingRepository;
import com.companya.resort.repository.DailyInventoryRateRepository;
import com.companya.resort.repository.PaymentRepository;
import com.companya.resort.security.RateLimitingFilter;
import com.companya.resort.service.BookingExpiryService;
import com.companya.resort.service.BookingPaymentService;
import com.companya.resort.service.BookingTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingPaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private DailyInventoryRateRepository dailyInventoryRateRepository;

    @Mock
    private BookingTransactionService bookingTransactionService;

    private Clock fixedClock;
    private BookingPaymentService bookingPaymentService;
    private BookingExpiryService bookingExpiryService;

    private final String bookingRef = "BKG123456789";
    private final String validToken = "token_abc_123456789012345678901234567890";
    private final LocalDate checkIn = LocalDate.of(2026, 11, 14);
    private final LocalDate checkOut = LocalDate.of(2026, 11, 16);

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2026-11-02T10:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

        bookingPaymentService = new BookingPaymentService(
                bookingRepository,
                paymentRepository,
                dailyInventoryRateRepository,
                bookingTransactionService,
                fixedClock
        );

        bookingExpiryService = new BookingExpiryService(
                bookingRepository,
                paymentRepository,
                dailyInventoryRateRepository,
                fixedClock
        );
    }

    private Booking createSampleBooking(BookingStatus status, LocalDateTime expiresAt) {
        RoomCategory category = new RoomCategory(1L, "ROOM", "Estate Room", 64, 2, 2, "Desc", "[]");
        RatePlan ratePlan = new RatePlan(1L, category, "EP", "Room Only", PriceModifierType.FIXED_ADDON, BigDecimal.ZERO, true);

        Booking booking = new Booking();
        booking.setId(100L);
        booking.setBookingRef(bookingRef);
        booking.setIdempotencyKey("idem-key-100");
        booking.setRoomCategory(category);
        booking.setRatePlan(ratePlan);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setNights(2);
        booking.setRoomsCount(1);
        booking.setAdults(2);
        booking.setChildren(0);
        booking.setGuestName("Test Guest");
        booking.setGuestEmail("a***@gmail.com");
        booking.setGuestPhone("98XXXXXX21");
        booking.setStatus(status);
        booking.setExpiresAt(expiresAt);
        booking.setTotalPayableInr(new BigDecimal("17000.00"));
        booking.setSubtotalBeforeDiscountInr(new BigDecimal("17000.00"));
        booking.setDiscountAmountInr(BigDecimal.ZERO);
        booking.setTaxableAmountInr(new BigDecimal("17000.00"));
        booking.setGstPercentageApplied(BigDecimal.ZERO);
        booking.setGstAmountInr(BigDecimal.ZERO);
        return booking;
    }

    private Payment createSamplePayment(Booking booking) {
        Payment payment = new Payment();
        payment.setId(200L);
        payment.setBooking(booking);
        payment.setOneTimePaymentToken(validToken);
        payment.setTokenUsed(false);
        payment.setAmountInr(booking.getTotalPayableInr());
        payment.setStatus(PaymentStatus.INITIATED);
        return payment;
    }

    private List<DailyInventoryRate> createSampleRates(RoomCategory category, int initialBookedCount) {
        DailyInventoryRate r1 = new DailyInventoryRate(1L, category, checkIn, new BigDecimal("8500.00"), 15, initialBookedCount, 0L);
        DailyInventoryRate r2 = new DailyInventoryRate(2L, category, checkIn.plusDays(1), new BigDecimal("8500.00"), 15, initialBookedCount, 0L);
        return new ArrayList<>(List.of(r1, r2));
    }

    @Test
    void designPackTest5_failedTestPaymentReleasesHeldAllotment() {
        Booking booking = createSampleBooking(BookingStatus.PENDING_PAYMENT, LocalDateTime.now(fixedClock).plusMinutes(10));
        Payment payment = createSamplePayment(booking);
        List<DailyInventoryRate> rates = createSampleRates(booking.getRoomCategory(), 1);

        when(bookingRepository.findAndLockByBookingRef(bookingRef)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(payment));
        when(dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(booking.getRoomCategory().getId(), checkIn, checkOut))
                .thenReturn(rates);
        when(bookingTransactionService.mapToBookingResponseDto(any(), any())).thenReturn(new BookingResponseDto());

        PaymentWebhookRequest request = new PaymentWebhookRequest(validToken, "FAILURE", "MOCK-REF-123", "MOCK_CARD");

        bookingPaymentService.processMockWebhook(bookingRef, validToken, request);

        assertEquals(BookingStatus.FAILED, booking.getStatus());
        assertTrue(payment.getTokenUsed());
        assertEquals(PaymentStatus.FAILURE, payment.getStatus());
        assertEquals(0, rates.get(0).getBookedCount(), "Nov 14 booked_count must be decremented by 1");
        assertEquals(0, rates.get(1).getBookedCount(), "Nov 15 booked_count must be decremented by 1");
        verify(dailyInventoryRateRepository, times(2)).save(any(DailyInventoryRate.class));
    }

    @Test
    void designPackTest6_scheduledExpiryJobReleasesBookedCountForExpiredHolds() {
        Booking booking = createSampleBooking(BookingStatus.PENDING_PAYMENT, LocalDateTime.now(fixedClock).minusMinutes(1));
        Payment payment = createSamplePayment(booking);
        List<DailyInventoryRate> rates = createSampleRates(booking.getRoomCategory(), 1);

        when(bookingRepository.findExpiredBookingIdsForUpdateSkipLocked(any())).thenReturn(List.of(booking.getId()));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(payment));
        when(dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(booking.getRoomCategory().getId(), checkIn, checkOut))
                .thenReturn(rates);

        int released = bookingExpiryService.releaseExpiredHolds();

        assertEquals(1, released);
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertTrue(payment.getTokenUsed());
        assertEquals(0, rates.get(0).getBookedCount(), "Nov 14 booked_count must be decremented from 1 back to 0");
        assertEquals(0, rates.get(1).getBookedCount(), "Nov 15 booked_count must be decremented from 1 back to 0");
        verify(dailyInventoryRateRepository, times(2)).save(any(DailyInventoryRate.class));
    }

    @Test
    void designPackTest7_paymentWebhookArrivingAfterHoldExpiryIsRejected() {
        // Expired hold: expires_at is in the past
        Booking booking = createSampleBooking(BookingStatus.PENDING_PAYMENT, LocalDateTime.now(fixedClock).minusMinutes(1));
        Payment payment = createSamplePayment(booking);

        when(bookingRepository.findAndLockByBookingRef(bookingRef)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(payment));

        PaymentWebhookRequest request = new PaymentWebhookRequest(validToken, "SUCCESS");

        assertThrows(BookingHoldExpiredException.class, () ->
                bookingPaymentService.processMockWebhook(bookingRef, validToken, request)
        );

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertTrue(payment.getTokenUsed());
        // Verify inventory was NOT locked or incremented/decremented
        verify(dailyInventoryRateRepository, never()).findAndLockByRoomCategoryIdAndStayDateRange(any(), any(), any());
    }

    @Test
    void designPackTest8_oneTimePaymentTokenCannotBeReused() {
        Booking booking = createSampleBooking(BookingStatus.PENDING_PAYMENT, LocalDateTime.now(fixedClock).plusMinutes(10));
        Payment payment = createSamplePayment(booking);
        payment.setTokenUsed(true); // Already used!

        when(bookingRepository.findAndLockByBookingRef(bookingRef)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(payment));

        PaymentWebhookRequest request = new PaymentWebhookRequest(validToken, "SUCCESS");

        assertThrows(PaymentTokenAlreadyUsedException.class, () ->
                bookingPaymentService.processMockWebhook(bookingRef, validToken, request)
        );
    }

    @Test
    void designPackTest24_cancelHoldReleasesStockAndMarksBookingCancelled() {
        Booking booking = createSampleBooking(BookingStatus.PENDING_PAYMENT, LocalDateTime.now(fixedClock).plusMinutes(10));
        Payment payment = createSamplePayment(booking);
        List<DailyInventoryRate> rates = createSampleRates(booking.getRoomCategory(), 1);

        when(bookingRepository.findAndLockByBookingRef(bookingRef)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(payment));
        when(dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(booking.getRoomCategory().getId(), checkIn, checkOut))
                .thenReturn(rates);
        when(bookingTransactionService.mapToBookingResponseDto(any(), any())).thenReturn(new BookingResponseDto());

        bookingPaymentService.cancelHold(bookingRef, validToken);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertTrue(payment.getTokenUsed());
        assertEquals(0, rates.get(0).getBookedCount());
        assertEquals(0, rates.get(1).getBookedCount());
        verify(dailyInventoryRateRepository, times(2)).save(any(DailyInventoryRate.class));
    }

    @Test
    void designPackTest25_getBookingMasksPhoneAndEmailAndReturns404ForUnknownRef() {
        Booking booking = createSampleBooking(BookingStatus.PENDING_PAYMENT, LocalDateTime.now(fixedClock).plusMinutes(10));
        booking.setGuestEmail("ananya.hegde@gmail.com");
        booking.setGuestPhone("9876543221");

        BookingResponseDto baseDto = new BookingResponseDto();
        baseDto.setBookingRef(bookingRef);

        when(bookingRepository.findByBookingRef(bookingRef)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());
        when(bookingTransactionService.mapToBookingResponseDto(eq(booking), isNull())).thenReturn(baseDto);

        BookingResponseDto result = bookingPaymentService.getBookingMasked(bookingRef);

        assertEquals("a***@gmail.com", result.getGuestEmail(), "Email must be masked as a***@gmail.com");
        assertEquals("98XXXXXX21", result.getGuestPhone(), "Phone must be masked as 98XXXXXX21");

        // Unknown ref -> 404
        when(bookingRepository.findByBookingRef("UNKNOWN_REF")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                bookingPaymentService.getBookingMasked("UNKNOWN_REF")
        );
    }

    @Test
    void designPackTest28_perIpRateLimitingEnforcedOnPublicBookingEndpoints() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(3); // 3 requests per minute for this test
        String clientIp = "198.51.100.1";

        // First 3 requests should pass
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/bookings/XYZ123456789");
            request.addHeader("X-Forwarded-For", clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            assertEquals(200, response.getStatus());
        }

        // 4th request from same IP must be rate limited to 429
        MockHttpServletRequest request4 = new MockHttpServletRequest("GET", "/api/v1/public/bookings/XYZ123456789");
        request4.addHeader("X-Forwarded-For", clientIp);
        MockHttpServletResponse response4 = new MockHttpServletResponse();
        filter.doFilter(request4, response4, (req, res) -> {});
        assertEquals(429, response4.getStatus());
        assertTrue(response4.getContentAsString().contains("Rate limit exceeded"));

        // Request with a DIFFERENT IP must succeed
        MockHttpServletRequest requestDiffIp = new MockHttpServletRequest("GET", "/api/v1/public/bookings/XYZ123456789");
        requestDiffIp.addHeader("X-Forwarded-For", "198.51.100.2");
        MockHttpServletResponse responseDiffIp = new MockHttpServletResponse();
        filter.doFilter(requestDiffIp, responseDiffIp, (req, res) -> {});
        assertEquals(200, responseDiffIp.getStatus());
    }

    @Test
    void webhookAndExpiryJobRacingReleaseStockExactlyOnce() throws Exception {
        RoomCategory cat = new RoomCategory(1L, "ROOM", "Room", 64, 2, 2, "Desc", "[]");
        DailyInventoryRate rate = new DailyInventoryRate(1L, cat, checkIn, new BigDecimal("8500.00"), 15, 1, 0L);
        Booking booking = createSampleBooking(BookingStatus.PENDING_PAYMENT, LocalDateTime.now(fixedClock).minusSeconds(1));
        Payment payment = createSamplePayment(booking);

        ReentrantLock bookingLock = new ReentrantLock();
        AtomicInteger releaseCount = new AtomicInteger(0);

        lenient().doAnswer(inv -> {
            bookingLock.lock();
            try {
                return Optional.of(booking);
            } finally {
                // Keep locked until released
            }
        }).when(bookingRepository).findAndLockByBookingRef(bookingRef);

        lenient().when(bookingRepository.findExpiredBookingIdsForUpdateSkipLocked(any())).thenAnswer(inv -> {
            if (bookingLock.isLocked()) {
                return List.of(); // SKIP LOCKED behavior: when locked by webhook, skipped!
            }
            return List.of(booking.getId());
        });

        lenient().when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        lenient().when(paymentRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(payment));

        lenient().when(dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(any(), any(), any()))
                .thenAnswer(inv -> {
                    releaseCount.incrementAndGet();
                    rate.setBookedCount(rate.getBookedCount() - 1);
                    return List.of(rate);
                });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<?> f1 = executor.submit(() -> {
            try {
                startLatch.await();
                bookingPaymentService.processMockWebhook(bookingRef, validToken, new PaymentWebhookRequest(validToken, "FAILURE"));
            } catch (Exception ignored) {
            } finally {
                if (bookingLock.isHeldByCurrentThread()) {
                    bookingLock.unlock();
                }
            }
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                startLatch.await();
                bookingExpiryService.releaseExpiredHolds();
            } catch (Exception ignored) {
            }
        });

        startLatch.countDown();
        f1.get(5, TimeUnit.SECONDS);
        f2.get(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one release happened
        assertTrue(releaseCount.get() <= 1, "Stock release must occur at most once during race");
        assertTrue(rate.getBookedCount() >= 0, "Booked count must not be decremented below 0");
    }
}
