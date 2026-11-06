package com.companya.resort.service;

import com.companya.resort.dto.BookingResponseDto;
import com.companya.resort.dto.CreateBookingRequest;
import com.companya.resort.dto.PriceCalculationResult;
import com.companya.resort.entity.*;
import com.companya.resort.exception.*;
import com.companya.resort.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class BookingTransactionService {

    private static final String ALPHANUMERIC = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private final SecureRandom secureRandom = new SecureRandom();

    private final RoomCategoryRepository roomCategoryRepository;
    private final RatePlanRepository ratePlanRepository;
    private final DailyInventoryRateRepository dailyInventoryRateRepository;
    private final BlackoutDateRepository blackoutDateRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PricingService pricingService;
    private final PromoCodeRepository promoCodeRepository;
    private final BookingCalendar bookingCalendar;
    private final Clock clock;

    @Autowired
    public BookingTransactionService(
            RoomCategoryRepository roomCategoryRepository,
            RatePlanRepository ratePlanRepository,
            DailyInventoryRateRepository dailyInventoryRateRepository,
            BlackoutDateRepository blackoutDateRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            PricingService pricingService,
            PromoCodeRepository promoCodeRepository,
            BookingCalendar bookingCalendar,
            Clock clock
    ) {
        this.roomCategoryRepository = roomCategoryRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.dailyInventoryRateRepository = dailyInventoryRateRepository;
        this.blackoutDateRepository = blackoutDateRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.pricingService = pricingService;
        this.promoCodeRepository = promoCodeRepository;
        this.bookingCalendar = bookingCalendar;
        this.clock = clock;
    }

    public BookingTransactionService(
            RoomCategoryRepository roomCategoryRepository,
            RatePlanRepository ratePlanRepository,
            DailyInventoryRateRepository dailyInventoryRateRepository,
            BlackoutDateRepository blackoutDateRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            PricingService pricingService,
            BookingCalendar bookingCalendar,
            Clock clock
    ) {
        this(roomCategoryRepository, ratePlanRepository, dailyInventoryRateRepository, blackoutDateRepository,
                bookingRepository, paymentRepository, pricingService, null, bookingCalendar, clock);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponseDto executeBookingTransaction(CreateBookingRequest request, String idempotencyKey) {
        // 1. Idempotency Check inside transaction
        Optional<Booking> existingBooking = bookingRepository.findByIdempotencyKey(idempotencyKey);
        if (existingBooking.isPresent()) {
            Booking existing = existingBooking.get();
            validatePayloadMatch(existing, request);
            Payment payment = paymentRepository.findByBookingId(existing.getId()).orElse(null);
            return mapToBookingResponseDto(existing, payment != null ? payment.getOneTimePaymentToken() : null);
        }

        // 2. Validation
        validateStayDates(request.getCheckInDate(), request.getCheckOutDate());

        if (Boolean.FALSE.equals(request.getDpdpConsentGiven())) {
            throw new IllegalArgumentException("DPDP Act 2023 consent is mandatory");
        }

        int roomsCount = request.getRoomsCount() != null ? request.getRoomsCount() : 1;
        if (roomsCount < 1) {
            throw new IllegalArgumentException("Rooms count must be at least 1");
        }

        RoomCategory category = roomCategoryRepository.findById(request.getRoomCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Room category not found with id: " + request.getRoomCategoryId()));

        RatePlan ratePlan = ratePlanRepository.findByIdAndIsActiveTrue(request.getRatePlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Active rate plan not found with id: " + request.getRatePlanId()));

        if (!ratePlan.getRoomCategory().getId().equals(category.getId())) {
            throw new IllegalArgumentException("Rate plan does not belong to the selected room category");
        }

        // 3. Validate Blackout Dates
        List<BlackoutDate> blackouts = blackoutDateRepository.findConflictingBlackouts(
                category.getId(), request.getCheckInDate(), request.getCheckOutDate());
        if (!blackouts.isEmpty()) {
            throw new BlackoutDateConflictException("Requested stay dates conflict with a resort blackout: " + blackouts.get(0).getReason());
        }

        // 4. Pessimistic Write Lock in Ascending Date Order
        List<DailyInventoryRate> lockedRates = dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(
                category.getId(), request.getCheckInDate(), request.getCheckOutDate());

        int nights = (int) ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (lockedRates.size() != nights) {
            throw new InsufficientAllotmentException("Inventory is not configured for all nights in the requested stay");
        }

        // 5. Verify Allotment Headroom for every night
        for (DailyInventoryRate rate : lockedRates) {
            if (rate.getBookedCount() + roomsCount > rate.getWebsiteAllotmentTotal()) {
                throw new InsufficientAllotmentException("Insufficient website allotment for date: " + rate.getStayDate());
            }
        }

        // 6. Active Hold Cap per Guest (inside transaction, after inventory locks)
        String email = request.getGuestEmail().trim().toLowerCase();
        String phone = request.getGuestPhone().trim();
        LocalDateTime now = LocalDateTime.now(clock);

        long activeHolds = bookingRepository.countActiveHoldsByGuest(email, phone, now);
        if (activeHolds >= 2) {
            throw new MaxActiveHoldsExceededException("Guest already has 2 active holds pending payment");
        }

        // 7. Increment Booked Count
        for (DailyInventoryRate rate : lockedRates) {
            rate.setBookedCount(rate.getBookedCount() + roomsCount);
            dailyInventoryRateRepository.save(rate);
        }

        // 8. Lock Promo Code under Pessimistic Write Lock (if provided) and Calculate Pricing
        PromoCode lockedPromo = null;
        if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()) {
            String promoCodeStr = request.getPromoCode().trim();
            if (promoCodeRepository != null) {
                lockedPromo = promoCodeRepository.findAndLockByCodeIgnoreCaseAndIsActiveTrue(promoCodeStr)
                        .orElseThrow(() -> new InvalidPromoCodeException("Promo code '" + promoCodeStr + "' is invalid or inactive"));
                int current = (lockedPromo.getCurrentUses() != null) ? lockedPromo.getCurrentUses() : 0;
                if (lockedPromo.getMaxUses() != null && current >= lockedPromo.getMaxUses()) {
                    throw new InvalidPromoCodeException("Promo code '" + promoCodeStr + "' has reached its maximum uses");
                }
            }
        }

        PriceCalculationResult priceResult;
        if (lockedPromo != null) {
            priceResult = pricingService.calculatePriceWithPromo(lockedRates, ratePlan, roomsCount, lockedPromo);
            int current = (lockedPromo.getCurrentUses() != null) ? lockedPromo.getCurrentUses() : 0;
            lockedPromo.setCurrentUses(current + 1);
            promoCodeRepository.save(lockedPromo);
        } else {
            priceResult = pricingService.calculatePrice(lockedRates, ratePlan, roomsCount, request.getPromoCode());
        }

        // 9. Generate Tokens and Create Booking + Payment
        String bookingRef = generateBookingRef();
        String paymentToken = generatePaymentToken();

        Booking booking = new Booking();
        booking.setBookingRef(bookingRef);
        booking.setIdempotencyKey(idempotencyKey);
        booking.setRoomCategory(category);
        booking.setRatePlan(ratePlan);
        booking.setPromoCode(priceResult.getPromoCode());
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNights(nights);
        booking.setRoomsCount(roomsCount);
        booking.setAdults(request.getAdults());
        booking.setChildren(request.getChildren() != null ? request.getChildren() : 0);
        booking.setGuestName(request.getGuestName().trim());
        booking.setGuestEmail(email);
        booking.setGuestPhone(phone);
        booking.setDpdpConsentGiven(true);
        booking.setDpdpConsentTimestamp(now);
        booking.setSubtotalBeforeDiscountInr(priceResult.getSubtotalBeforeDiscountInr());
        booking.setDiscountAmountInr(priceResult.getDiscountAmountInr());
        booking.setTaxableAmountInr(priceResult.getTaxableAmountInr());
        booking.setGstPercentageApplied(priceResult.getGstPercentageApplied());
        booking.setGstAmountInr(priceResult.getGstAmountInr());
        booking.setTotalPayableInr(priceResult.getTotalPayableInr());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setExpiresAt(now.plusMinutes(10));

        Booking savedBooking = bookingRepository.save(booking);

        Payment payment = new Payment();
        payment.setBooking(savedBooking);
        payment.setOneTimePaymentToken(paymentToken);
        payment.setTokenUsed(false);
        payment.setAmountInr(priceResult.getTotalPayableInr());
        payment.setStatus(PaymentStatus.INITIATED);
        paymentRepository.save(payment);

        return mapToBookingResponseDto(savedBooking, paymentToken);
    }

    public void validatePayloadMatch(Booking existing, CreateBookingRequest request) {
        int requestedRooms = request.getRoomsCount() != null ? request.getRoomsCount() : 1;
        int requestedAdults = request.getAdults() != null ? request.getAdults() : 1;
        int requestedChildren = request.getChildren() != null ? request.getChildren() : 0;

        boolean matches = existing.getRoomCategory().getId().equals(request.getRoomCategoryId())
                && existing.getRatePlan().getId().equals(request.getRatePlanId())
                && existing.getCheckInDate().equals(request.getCheckInDate())
                && existing.getCheckOutDate().equals(request.getCheckOutDate())
                && existing.getRoomsCount().equals(requestedRooms)
                && existing.getAdults().equals(requestedAdults)
                && existing.getChildren().equals(requestedChildren)
                && existing.getGuestEmail().equalsIgnoreCase(request.getGuestEmail().trim());

        String existingPromo = existing.getPromoCode() != null ? existing.getPromoCode().getCode() : null;
        String requestedPromo = (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty())
                ? request.getPromoCode().trim() : null;
        boolean promoMatches = (existingPromo == null && requestedPromo == null)
                || (existingPromo != null && existingPromo.equalsIgnoreCase(requestedPromo));

        if (!matches || !promoMatches) {
            throw new IdempotencyPayloadMismatchException("Idempotency key already used with different booking payload");
        }
    }

    public void validateStayDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new InvalidDateRangeException("Check-in and check-out dates are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidDateRangeException("Check-out date must be strictly after check-in date");
        }
        LocalDate earliestAllowed = bookingCalendar.today();
        if (checkIn.isBefore(earliestAllowed)) {
            throw new InvalidDateRangeException("Check-in date cannot be in the past");
        }
    }

    public BookingResponseDto mapToBookingResponseDto(Booking booking, String paymentToken) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setBookingRef(booking.getBookingRef());
        dto.setIdempotencyKey(booking.getIdempotencyKey());
        dto.setStatus(booking.getStatus().name());
        dto.setRoomCategoryId(booking.getRoomCategory().getId());
        dto.setRoomCategoryName(booking.getRoomCategory().getName());
        dto.setRatePlanId(booking.getRatePlan().getId());
        dto.setRatePlanName(booking.getRatePlan().getName());
        dto.setCheckInDate(booking.getCheckInDate());
        dto.setCheckOutDate(booking.getCheckOutDate());
        dto.setNights(booking.getNights());
        dto.setRoomsCount(booking.getRoomsCount());
        dto.setAdults(booking.getAdults());
        dto.setChildren(booking.getChildren());
        dto.setGuestName(booking.getGuestName());
        dto.setGuestEmail(booking.getGuestEmail());
        dto.setGuestPhone(booking.getGuestPhone());
        dto.setSubtotalBeforeDiscountInr(booking.getSubtotalBeforeDiscountInr());
        dto.setDiscountAmountInr(booking.getDiscountAmountInr());
        dto.setTaxableAmountInr(booking.getTaxableAmountInr());
        dto.setGstPercentageApplied(booking.getGstPercentageApplied());
        dto.setGstAmountInr(booking.getGstAmountInr());
        dto.setTotalPayableInr(booking.getTotalPayableInr());
        dto.setGstVerificationStatus("UNVERIFIED");
        dto.setExpiresAt(booking.getExpiresAt());
        dto.setOneTimePaymentToken(paymentToken);
        return dto;
    }

    private String generateBookingRef() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(ALPHANUMERIC.charAt(secureRandom.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    private String generatePaymentToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
