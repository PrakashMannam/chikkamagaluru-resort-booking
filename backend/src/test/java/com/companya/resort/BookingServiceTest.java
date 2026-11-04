package com.companya.resort;

import com.companya.resort.dto.AvailabilityResponse;
import com.companya.resort.dto.BookingResponseDto;
import com.companya.resort.dto.CreateBookingRequest;
import com.companya.resort.dto.RoomCategoryAvailabilityDto;
import com.companya.resort.entity.*;
import com.companya.resort.exception.*;
import com.companya.resort.repository.*;
import com.companya.resort.service.BookingCalendar;
import com.companya.resort.service.BookingService;
import com.companya.resort.service.BookingTransactionService;
import com.companya.resort.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private RoomCategoryRepository roomCategoryRepository;

    @Mock
    private RatePlanRepository ratePlanRepository;

    @Mock
    private DailyInventoryRateRepository dailyInventoryRateRepository;

    @Mock
    private BlackoutDateRepository blackoutDateRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private GstTaxSlabRepository gstTaxSlabRepository;

    @Mock
    private PromoCodeRepository promoCodeRepository;

    private Clock fixedClock;
    private BookingCalendar bookingCalendar;
    private PricingService pricingService;
    private BookingTransactionService transactionService;
    private BookingService bookingService;

    private RoomCategory roomCategory;
    private RatePlan ratePlan;
    private LocalDate checkIn;
    private LocalDate checkOut;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
                LocalDate.of(2026, 11, 2).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
        bookingCalendar = new BookingCalendar("2026-11-02", fixedClock);
        pricingService = new PricingService(gstTaxSlabRepository, promoCodeRepository, fixedClock);
        transactionService = new BookingTransactionService(
                roomCategoryRepository,
                ratePlanRepository,
                dailyInventoryRateRepository,
                blackoutDateRepository,
                bookingRepository,
                paymentRepository,
                pricingService,
                bookingCalendar,
                fixedClock
        );
        bookingService = new BookingService(
                transactionService,
                roomCategoryRepository,
                ratePlanRepository,
                dailyInventoryRateRepository,
                blackoutDateRepository,
                bookingRepository,
                paymentRepository,
                pricingService,
                bookingCalendar
        );

        roomCategory = new RoomCategory(1L, "ROOM", "Coffee Estate Canopy Room", 64, 2, 2,
                "Description", "[\"https://placehold.co/800x600\"]");
        ratePlan = new RatePlan(1L, roomCategory, "EP", "European Plan",
                PriceModifierType.FIXED_ADDON, BigDecimal.ZERO, true);

        checkIn = LocalDate.of(2026, 11, 14);
        checkOut = LocalDate.of(2026, 11, 16); // 2 nights
    }

    @Test
    void successfulAvailabilityLookup() {
        when(roomCategoryRepository.findAll()).thenReturn(List.of(roomCategory));
        when(blackoutDateRepository.findAllBlackoutsInRange(checkIn, checkOut)).thenReturn(Collections.emptyList());

        DailyInventoryRate rateDay1 = new DailyInventoryRate(1L, roomCategory, checkIn, new BigDecimal("8500.00"), 10, 2, 0L);
        DailyInventoryRate rateDay2 = new DailyInventoryRate(2L, roomCategory, checkIn.plusDays(1), new BigDecimal("8500.00"), 10, 3, 0L);

        when(dailyInventoryRateRepository.findByRoomCategoryIdAndStayDateRange(1L, checkIn, checkOut))
                .thenReturn(List.of(rateDay1, rateDay2));
        when(ratePlanRepository.findByRoomCategoryIdAndIsActiveTrue(1L)).thenReturn(List.of(ratePlan));

        GstTaxSlab slab = new GstTaxSlab(1L, BigDecimal.ZERO, null, BigDecimal.ZERO, LocalDate.of(2026, 11, 1), null, GstVerificationStatus.UNVERIFIED);
        when(gstTaxSlabRepository.findMatchingSlabs(any(LocalDate.class), any(BigDecimal.class)))
                .thenReturn(List.of(slab));

        AvailabilityResponse response = bookingService.checkAvailability(checkIn, checkOut, 1, 2, 0);

        assertNotNull(response);
        assertEquals(2, response.getNights());
        assertEquals(1, response.getCategories().size());

        RoomCategoryAvailabilityDto catDto = response.getCategories().get(0);
        assertEquals("ROOM", catDto.getCode());
        assertFalse(catDto.getIsBlackedOut());
        assertTrue(catDto.getHasAllotment());
        assertEquals(7, catDto.getRemainingAllotment()); // min(10-2, 10-3) = 7
        assertEquals(1, catDto.getRatePlans().size());
        assertEquals(new BigDecimal("17000.00"), catDto.getRatePlans().get(0).getTotalPayableInr());
    }

    @Test
    void availabilityLookup_withInvalidDateRanges_throwsException() {
        // checkOut before checkIn
        assertThrows(InvalidDateRangeException.class, () ->
                bookingService.checkAvailability(checkIn, checkIn.minusDays(1), 1, 2, 0));

        // checkIn equals checkOut
        assertThrows(InvalidDateRangeException.class, () ->
                bookingService.checkAvailability(checkIn, checkIn, 1, 2, 0));

        // checkIn in the past relative to bookingCalendar.today() (2026-11-02)
        assertThrows(InvalidDateRangeException.class, () ->
                bookingService.checkAvailability(LocalDate.of(2026, 11, 1), checkOut, 1, 2, 0));

        // null dates
        assertThrows(InvalidDateRangeException.class, () ->
                bookingService.checkAvailability(null, checkOut, 1, 2, 0));
    }

    @Test
    void availabilityLookup_respectsBlackoutDates() {
        when(roomCategoryRepository.findAll()).thenReturn(List.of(roomCategory));
        BlackoutDate blackout = new BlackoutDate(1L, roomCategory, checkIn, "Full Resort Buyout");
        when(blackoutDateRepository.findAllBlackoutsInRange(checkIn, checkOut)).thenReturn(List.of(blackout));
        when(dailyInventoryRateRepository.findByRoomCategoryIdAndStayDateRange(1L, checkIn, checkOut))
                .thenReturn(Collections.emptyList());

        AvailabilityResponse response = bookingService.checkAvailability(checkIn, checkOut, 1, 2, 0);

        RoomCategoryAvailabilityDto catDto = response.getCategories().get(0);
        assertTrue(catDto.getIsBlackedOut());
        assertEquals("Full Resort Buyout", catDto.getBlackoutReason());
        assertFalse(catDto.getHasAllotment());
    }

    @Test
    void availabilityLookup_respectsSoldOutInventory() {
        when(roomCategoryRepository.findAll()).thenReturn(List.of(roomCategory));
        when(blackoutDateRepository.findAllBlackoutsInRange(checkIn, checkOut)).thenReturn(Collections.emptyList());

        // Day 2 has 0 remaining allotment
        DailyInventoryRate rateDay1 = new DailyInventoryRate(1L, roomCategory, checkIn, new BigDecimal("8500.00"), 5, 2, 0L);
        DailyInventoryRate rateDay2 = new DailyInventoryRate(2L, roomCategory, checkIn.plusDays(1), new BigDecimal("8500.00"), 5, 5, 0L);

        when(dailyInventoryRateRepository.findByRoomCategoryIdAndStayDateRange(1L, checkIn, checkOut))
                .thenReturn(List.of(rateDay1, rateDay2));
        when(ratePlanRepository.findByRoomCategoryIdAndIsActiveTrue(1L)).thenReturn(List.of(ratePlan));

        AvailabilityResponse response = bookingService.checkAvailability(checkIn, checkOut, 1, 2, 0);

        RoomCategoryAvailabilityDto catDto = response.getCategories().get(0);
        assertFalse(catDto.getIsBlackedOut());
        assertFalse(catDto.getHasAllotment(), "Category must not have allotment if any night is sold out");
        assertEquals(0, catDto.getRemainingAllotment());
    }

    @Test
    void createBooking_success() {
        CreateBookingRequest request = new CreateBookingRequest(
                "idem-12345", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Vikram Sharma", "vikram@example.com", "9876543210", null, true
        );

        when(bookingRepository.findByIdempotencyKey("idem-12345")).thenReturn(Optional.empty());
        when(roomCategoryRepository.findById(1L)).thenReturn(Optional.of(roomCategory));
        when(ratePlanRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(ratePlan));
        when(blackoutDateRepository.findConflictingBlackouts(1L, checkIn, checkOut)).thenReturn(Collections.emptyList());

        DailyInventoryRate rateDay1 = new DailyInventoryRate(1L, roomCategory, checkIn, new BigDecimal("8500.00"), 5, 1, 0L);
        DailyInventoryRate rateDay2 = new DailyInventoryRate(2L, roomCategory, checkIn.plusDays(1), new BigDecimal("8500.00"), 5, 1, 0L);

        when(dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(1L, checkIn, checkOut))
                .thenReturn(List.of(rateDay1, rateDay2));
        when(bookingRepository.countActiveHoldsByGuest(eq("vikram@example.com"), eq("9876543210"), any(LocalDateTime.class))).thenReturn(0L);

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(100L);
            return b;
        });

        BookingResponseDto response = bookingService.createBookingAndLockAllotment(request, "idem-12345");

        assertNotNull(response);
        assertEquals(12, response.getBookingRef().length(), "Booking ref must be 12 characters");
        assertEquals("PENDING_PAYMENT", response.getStatus());
        assertEquals("idem-12345", response.getIdempotencyKey());
        assertEquals(64, response.getOneTimePaymentToken().length(), "Payment token must be 64 characters");
        assertEquals(new BigDecimal("17000.00"), response.getTotalPayableInr());

        // Verify allotment was incremented
        assertEquals(2, rateDay1.getBookedCount());
        assertEquals(2, rateDay2.getBookedCount());
        verify(dailyInventoryRateRepository, times(2)).save(any(DailyInventoryRate.class));
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void createBooking_duplicateIdempotencyKey_samePayload_returnsExistingBookingWithoutIncrementing() {
        Booking existing = new Booking();
        existing.setId(50L);
        existing.setBookingRef("REF123456789");
        existing.setIdempotencyKey("idem-duplicate");
        existing.setRoomCategory(roomCategory);
        existing.setRatePlan(ratePlan);
        existing.setCheckInDate(checkIn);
        existing.setCheckOutDate(checkOut);
        existing.setNights(2);
        existing.setRoomsCount(1);
        existing.setAdults(2);
        existing.setChildren(0);
        existing.setGuestName("Vikram");
        existing.setGuestEmail("vikram@example.com");
        existing.setGuestPhone("9876543210");
        existing.setSubtotalBeforeDiscountInr(new BigDecimal("17000.00"));
        existing.setDiscountAmountInr(BigDecimal.ZERO);
        existing.setTaxableAmountInr(new BigDecimal("17000.00"));
        existing.setGstPercentageApplied(BigDecimal.ZERO);
        existing.setGstAmountInr(BigDecimal.ZERO);
        existing.setTotalPayableInr(new BigDecimal("17000.00"));
        existing.setStatus(BookingStatus.PENDING_PAYMENT);
        existing.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        Payment payment = new Payment();
        payment.setBooking(existing);
        payment.setOneTimePaymentToken("token-existing-12345");

        when(bookingRepository.findByIdempotencyKey("idem-duplicate")).thenReturn(Optional.of(existing));
        when(paymentRepository.findByBookingId(50L)).thenReturn(Optional.of(payment));

        CreateBookingRequest request = new CreateBookingRequest(
                "idem-duplicate", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Vikram", "vikram@example.com", "9876543210", null, true
        );

        BookingResponseDto response = bookingService.createBookingAndLockAllotment(request, "idem-duplicate");

        assertNotNull(response);
        assertEquals("REF123456789", response.getBookingRef());
        assertEquals("token-existing-12345", response.getOneTimePaymentToken());

        // Must not lock or save any daily inventory rates or new bookings
        verify(dailyInventoryRateRepository, never()).findAndLockByRoomCategoryIdAndStayDateRange(anyLong(), any(), any());
        verify(dailyInventoryRateRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_duplicateIdempotencyKey_differentPayload_throws422() {
        Booking existing = new Booking();
        existing.setId(50L);
        existing.setBookingRef("REF123456789");
        existing.setIdempotencyKey("idem-duplicate");
        existing.setRoomCategory(roomCategory);
        existing.setRatePlan(ratePlan);
        existing.setCheckInDate(checkIn);
        existing.setCheckOutDate(checkOut);
        existing.setNights(2);
        existing.setRoomsCount(1);
        existing.setAdults(2);
        existing.setChildren(0);
        existing.setGuestName("Vikram");
        existing.setGuestEmail("vikram@example.com");
        existing.setGuestPhone("9876543210");

        when(bookingRepository.findByIdempotencyKey("idem-duplicate")).thenReturn(Optional.of(existing));

        // Different email "different@example.com"
        CreateBookingRequest request = new CreateBookingRequest(
                "idem-duplicate", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Vikram", "different@example.com", "9876543210", null, true
        );

        assertThrows(IdempotencyPayloadMismatchException.class, () ->
                bookingService.createBookingAndLockAllotment(request, "idem-duplicate"));
    }

    @Test
    void createBooking_activeHoldCapExceeded_checkedInsideTransactionAfterLocks() {
        CreateBookingRequest request = new CreateBookingRequest(
                "idem-hold-cap", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Greedy Guest", "greedy@example.com", "9876543210", null, true
        );

        when(bookingRepository.findByIdempotencyKey("idem-hold-cap")).thenReturn(Optional.empty());
        when(roomCategoryRepository.findById(1L)).thenReturn(Optional.of(roomCategory));
        when(ratePlanRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(ratePlan));
        when(blackoutDateRepository.findConflictingBlackouts(1L, checkIn, checkOut)).thenReturn(Collections.emptyList());

        DailyInventoryRate rateDay1 = new DailyInventoryRate(1L, roomCategory, checkIn, new BigDecimal("8500.00"), 5, 1, 0L);
        DailyInventoryRate rateDay2 = new DailyInventoryRate(2L, roomCategory, checkIn.plusDays(1), new BigDecimal("8500.00"), 5, 1, 0L);

        when(dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(1L, checkIn, checkOut))
                .thenReturn(List.of(rateDay1, rateDay2));

        when(bookingRepository.countActiveHoldsByGuest(eq("greedy@example.com"), eq("9876543210"), any(LocalDateTime.class)))
                .thenReturn(2L); // Already at cap

        assertThrows(MaxActiveHoldsExceededException.class, () ->
                bookingService.createBookingAndLockAllotment(request, "idem-hold-cap"));

        // Row locks were acquired, but allotment was NOT incremented and booking NOT saved
        assertEquals(1, rateDay1.getBookedCount());
        assertEquals(1, rateDay2.getBookedCount());
        verify(dailyInventoryRateRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_retryOnTransientLockException_exhaustsRetriesAndThrows503() {
        CreateBookingRequest request = new CreateBookingRequest(
                "idem-lock-retry", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Guest", "guest@example.com", "9876543210", null, true
        );

        when(bookingRepository.findByIdempotencyKey("idem-lock-retry")).thenReturn(Optional.empty());
        when(roomCategoryRepository.findById(1L)).thenReturn(Optional.of(roomCategory));
        when(ratePlanRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(ratePlan));
        when(blackoutDateRepository.findConflictingBlackouts(1L, checkIn, checkOut)).thenReturn(Collections.emptyList());

        when(dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(1L, checkIn, checkOut))
                .thenThrow(new org.springframework.dao.CannotAcquireLockException("Lock wait timeout exceeded"));

        assertThrows(ServiceUnavailableException.class, () ->
                bookingService.createBookingAndLockAllotment(request, "idem-lock-retry"));

        // Initial attempt + 3 retries = 4 invocations
        verify(dailyInventoryRateRepository, times(4))
                .findAndLockByRoomCategoryIdAndStayDateRange(1L, checkIn, checkOut);
    }

    @Test
    void createBooking_invalidCategoryOrRatePlan_throwsException() {
        CreateBookingRequest requestUnknownCategory = new CreateBookingRequest(
                "idem-unknown-cat", 999L, 1L, checkIn, checkOut, 1, 2, 0,
                "Guest", "guest@example.com", "9876543210", null, true
        );

        when(bookingRepository.findByIdempotencyKey("idem-unknown-cat")).thenReturn(Optional.empty());
        when(roomCategoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                bookingService.createBookingAndLockAllotment(requestUnknownCategory, "idem-unknown-cat"));
    }

    @Test
    void createBooking_blackoutDates_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest(
                "idem-blackout", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Guest", "guest@example.com", "9876543210", null, true
        );

        when(bookingRepository.findByIdempotencyKey("idem-blackout")).thenReturn(Optional.empty());
        when(roomCategoryRepository.findById(1L)).thenReturn(Optional.of(roomCategory));
        when(ratePlanRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(ratePlan));

        BlackoutDate blackout = new BlackoutDate(1L, null, checkIn, "Christmas Buyout");
        when(blackoutDateRepository.findConflictingBlackouts(1L, checkIn, checkOut)).thenReturn(List.of(blackout));

        assertThrows(BlackoutDateConflictException.class, () ->
                bookingService.createBookingAndLockAllotment(request, "idem-blackout"));

        verify(dailyInventoryRateRepository, never()).findAndLockByRoomCategoryIdAndStayDateRange(anyLong(), any(), any());
    }

    @Test
    void createBooking_soldOutInventory_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest(
                "idem-sold-out", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Guest", "guest@example.com", "9876543210", null, true
        );

        when(bookingRepository.findByIdempotencyKey("idem-sold-out")).thenReturn(Optional.empty());
        when(roomCategoryRepository.findById(1L)).thenReturn(Optional.of(roomCategory));
        when(ratePlanRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(ratePlan));
        when(blackoutDateRepository.findConflictingBlackouts(1L, checkIn, checkOut)).thenReturn(Collections.emptyList());

        DailyInventoryRate rateDay1 = new DailyInventoryRate(1L, roomCategory, checkIn, new BigDecimal("8500.00"), 5, 5, 0L); // 5 of 5 booked!
        DailyInventoryRate rateDay2 = new DailyInventoryRate(2L, roomCategory, checkIn.plusDays(1), new BigDecimal("8500.00"), 5, 2, 0L);

        when(dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(1L, checkIn, checkOut))
                .thenReturn(List.of(rateDay1, rateDay2));

        assertThrows(InsufficientAllotmentException.class, () ->
                bookingService.createBookingAndLockAllotment(request, "idem-sold-out"));

        // Must not save any changes
        verify(dailyInventoryRateRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_partialStayFailure_rollsBackAllotment() {
        CreateBookingRequest request = new CreateBookingRequest(
                "idem-partial", 1L, 1L, checkIn, checkOut, 2, 2, 0, // requesting 2 rooms
                "Guest", "guest@example.com", "9876543210", null, true
        );

        when(bookingRepository.findByIdempotencyKey("idem-partial")).thenReturn(Optional.empty());
        when(roomCategoryRepository.findById(1L)).thenReturn(Optional.of(roomCategory));
        when(ratePlanRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(ratePlan));
        when(blackoutDateRepository.findConflictingBlackouts(1L, checkIn, checkOut)).thenReturn(Collections.emptyList());

        // Day 1 has room for 2 (1 + 2 <= 5)
        // Day 2 has room for ONLY 1 (4 + 2 > 5) -> FAILS!
        DailyInventoryRate rateDay1 = new DailyInventoryRate(1L, roomCategory, checkIn, new BigDecimal("8500.00"), 5, 1, 0L);
        DailyInventoryRate rateDay2 = new DailyInventoryRate(2L, roomCategory, checkIn.plusDays(1), new BigDecimal("8500.00"), 5, 4, 0L);

        when(dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(1L, checkIn, checkOut))
                .thenReturn(List.of(rateDay1, rateDay2));

        assertThrows(InsufficientAllotmentException.class, () ->
                bookingService.createBookingAndLockAllotment(request, "idem-partial"));

        // Verify neither rate was incremented or saved
        assertEquals(1, rateDay1.getBookedCount(), "Day 1 booked count must NOT be incremented if day 2 fails");
        assertEquals(4, rateDay2.getBookedCount(), "Day 2 booked count must NOT be modified");
        verify(dailyInventoryRateRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_concurrencySimulation_onlyOneSucceedsForLastUnit() throws InterruptedException {
        // Simulated concurrent reservation for the 1 remaining room unit
        ReentrantLock dbRowLock = new ReentrantLock();
        DailyInventoryRate sharedRate = new DailyInventoryRate(1L, roomCategory, checkIn, new BigDecimal("8500.00"), 1, 0, 0L);

        when(roomCategoryRepository.findById(1L)).thenReturn(Optional.of(roomCategory));
        when(ratePlanRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(ratePlan));
        when(blackoutDateRepository.findConflictingBlackouts(eq(1L), any(), any())).thenReturn(Collections.emptyList());

        LocalDate singleNightCheckOut = checkIn.plusDays(1);

        when(dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(1L, checkIn, singleNightCheckOut))
                .thenAnswer(invocation -> {
                    dbRowLock.lock(); // Simulate pessimistic row lock acquisition
                    return List.of(sharedRate);
                });

        doAnswer(invocation -> {
            dbRowLock.unlock(); // Release lock when transaction commits
            return invocation.getArgument(0);
        }).when(dailyInventoryRateRepository).save(any(DailyInventoryRate.class));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startGate.await();
                    CreateBookingRequest request = new CreateBookingRequest(
                            "idem-concurrent-" + idx, 1L, 1L, checkIn, singleNightCheckOut, 1, 2, 0,
                            "Guest " + idx, "guest" + idx + "@test.com", "987654321" + idx, null, true
                    );
                    bookingService.createBookingAndLockAllotment(request, "idem-concurrent-" + idx);
                    successCount.incrementAndGet();
                } catch (InsufficientAllotmentException e) {
                    failureCount.incrementAndGet();
                    if (dbRowLock.isHeldByCurrentThread()) {
                        dbRowLock.unlock(); // Simulate rollback releasing lock
                    }
                } catch (Exception e) {
                    if (dbRowLock.isHeldByCurrentThread()) {
                        dbRowLock.unlock();
                    }
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(endGate.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly 1 concurrent thread must succeed for the last available room");
        assertEquals(1, failureCount.get(), "The competing thread must fail with InsufficientAllotmentException");
        assertEquals(1, sharedRate.getBookedCount(), "Booked count must never exceed allotment cap");
    }

    @Test
    void createBooking_withFixedTodaySet_stillGetsExpiresAtEqualRealNowPlus10Minutes() {
        // fixed-today is set to 2026-11-02, but Clock is the system clock
        Clock systemClock = Clock.systemDefaultZone();
        BookingCalendar calWithFixedToday = new BookingCalendar("2026-11-02", systemClock);

        BookingTransactionService customTxService = new BookingTransactionService(
                roomCategoryRepository, ratePlanRepository, dailyInventoryRateRepository,
                blackoutDateRepository, bookingRepository, paymentRepository, pricingService,
                calWithFixedToday, systemClock
        );
        BookingService customBookingService = new BookingService(
                customTxService, roomCategoryRepository, ratePlanRepository,
                dailyInventoryRateRepository, blackoutDateRepository, bookingRepository,
                paymentRepository, pricingService, calWithFixedToday
        );

        LocalDate stayCheckIn = LocalDate.of(2026, 11, 14);
        LocalDate stayCheckOut = LocalDate.of(2026, 11, 16);

        CreateBookingRequest request = new CreateBookingRequest(
                "idem-clock-verify", 1L, 1L, stayCheckIn, stayCheckOut, 1, 2, 0,
                "Real Clock Guest", "realclock@example.com", "9876543210", null, true
        );

        when(bookingRepository.findByIdempotencyKey("idem-clock-verify")).thenReturn(Optional.empty());
        when(roomCategoryRepository.findById(1L)).thenReturn(Optional.of(roomCategory));
        when(ratePlanRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(ratePlan));
        when(blackoutDateRepository.findConflictingBlackouts(1L, stayCheckIn, stayCheckOut)).thenReturn(Collections.emptyList());

        DailyInventoryRate r1 = new DailyInventoryRate(1L, roomCategory, stayCheckIn, new BigDecimal("8500.00"), 5, 0, 0L);
        DailyInventoryRate r2 = new DailyInventoryRate(2L, roomCategory, stayCheckIn.plusDays(1), new BigDecimal("8500.00"), 5, 0, 0L);
        when(dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(1L, stayCheckIn, stayCheckOut))
                .thenReturn(List.of(r1, r2));

        LocalDateTime beforeTime = LocalDateTime.now(systemClock);

        final Booking[] captured = new Booking[1];
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            captured[0] = invocation.getArgument(0);
            captured[0].setId(999L);
            return captured[0];
        });

        customBookingService.createBookingAndLockAllotment(request, "idem-clock-verify");

        LocalDateTime afterTime = LocalDateTime.now(systemClock);

        assertNotNull(captured[0]);
        LocalDateTime expiresAt = captured[0].getExpiresAt();
        assertNotNull(expiresAt);

        // Assert expires_at is ~ real now + 10 minutes (NOT 2026-11-02 + 10 minutes)
        LocalDateTime expectedMin = beforeTime.plusMinutes(10).minusSeconds(2);
        LocalDateTime expectedMax = afterTime.plusMinutes(10).plusSeconds(2);
        assertTrue(expiresAt.isAfter(expectedMin) && expiresAt.isBefore(expectedMax),
                "expires_at (" + expiresAt + ") must be within real now + 10 minutes range (" + expectedMin + " to " + expectedMax + ")");
    }
}
