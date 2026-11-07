package com.companya.resort.service;

import com.companya.resort.dto.*;
import com.companya.resort.entity.*;
import com.companya.resort.exception.*;
import com.companya.resort.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class BookingService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final BookingTransactionService transactionService;
    private final RoomCategoryRepository roomCategoryRepository;
    private final RatePlanRepository ratePlanRepository;
    private final DailyInventoryRateRepository dailyInventoryRateRepository;
    private final BlackoutDateRepository blackoutDateRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PricingService pricingService;
    private final BookingCalendar bookingCalendar;

    public BookingService(
            BookingTransactionService transactionService,
            RoomCategoryRepository roomCategoryRepository,
            RatePlanRepository ratePlanRepository,
            DailyInventoryRateRepository dailyInventoryRateRepository,
            BlackoutDateRepository blackoutDateRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            PricingService pricingService,
            BookingCalendar bookingCalendar
    ) {
        this.transactionService = transactionService;
        this.roomCategoryRepository = roomCategoryRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.dailyInventoryRateRepository = dailyInventoryRateRepository;
        this.blackoutDateRepository = blackoutDateRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.pricingService = pricingService;
        this.bookingCalendar = bookingCalendar;
    }

    public AvailabilityResponse checkAvailability(
            LocalDate checkIn,
            LocalDate checkOut,
            int rooms,
            int adults,
            int children
    ) {
        transactionService.validateStayDates(checkIn, checkOut);

        if (rooms < 1) {
            throw new IllegalArgumentException("Rooms must be at least 1");
        }
        if (adults < 1) {
            throw new IllegalArgumentException("Adults must be at least 1");
        }
        if (children < 0) {
            throw new IllegalArgumentException("Children cannot be negative");
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        List<RoomCategory> categories = roomCategoryRepository.findAll();
        List<BlackoutDate> allBlackouts = blackoutDateRepository.findAllBlackoutsInRange(checkIn, checkOut);

        List<RoomCategoryAvailabilityDto> categoryDtos = new ArrayList<>();

        for (RoomCategory category : categories) {
            RoomCategoryAvailabilityDto dto = new RoomCategoryAvailabilityDto();
            dto.setId(category.getId());
            dto.setCode(category.getCode());
            dto.setName(category.getName());
            dto.setTotalPhysicalKeys(category.getTotalPhysicalKeys());
            dto.setMaxAdults(category.getMaxAdults());
            dto.setMaxChildren(category.getMaxChildren());
            dto.setBaseDescription(category.getBaseDescription());
            dto.setPhotoUrls(parsePhotoUrls(category.getPhotoUrlsJson()));

            // Blackout check
            Optional<BlackoutDate> blackoutMatch = allBlackouts.stream()
                    .filter(b -> b.getRoomCategory() == null || b.getRoomCategory().getId().equals(category.getId()))
                    .findFirst();

            if (blackoutMatch.isPresent()) {
                dto.setIsBlackedOut(true);
                dto.setBlackoutReason(blackoutMatch.get().getReason());
            } else {
                dto.setIsBlackedOut(false);
                dto.setBlackoutReason(null);
            }

            // Daily inventory rates check
            List<DailyInventoryRate> rates = dailyInventoryRateRepository
                    .findByRoomCategoryIdAndStayDateRange(category.getId(), checkIn, checkOut);

            boolean completeCoverage = rates.size() == nights;
            int remainingAllotment = 0;

            if (completeCoverage) {
                remainingAllotment = rates.stream()
                        .mapToInt(r -> Math.max(0, r.getWebsiteAllotmentTotal() - r.getBookedCount()))
                        .min()
                        .orElse(0);
            }

            dto.setRemainingAllotment(remainingAllotment);
            dto.setHasAllotment(!dto.getIsBlackedOut() && completeCoverage && remainingAllotment >= rooms);

            // Starting rate
            if (!rates.isEmpty()) {
                BigDecimal minBase = rates.stream()
                        .map(DailyInventoryRate::getBaseRateInr)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                dto.setStartingRatePerNight(minBase);
            } else {
                dto.setStartingRatePerNight(BigDecimal.ZERO);
            }

            // Rate plans with calculated prices
            List<RatePlan> ratePlans = ratePlanRepository.findByRoomCategoryIdAndIsActiveTrue(category.getId());
            List<RatePlanPriceDto> planDtos = new ArrayList<>();

            if (completeCoverage) {
                for (RatePlan plan : ratePlans) {
                    PriceCalculationResult priceResult = pricingService.calculatePrice(rates, plan, rooms, null);
                    RatePlanPriceDto planDto = new RatePlanPriceDto(
                            plan.getId(),
                            plan.getCode(),
                            plan.getName(),
                            plan.getPriceModifierType().name(),
                            plan.getPriceModifierValue(),
                            priceResult.getSubtotalBeforeDiscountInr(),
                            priceResult.getTaxableAmountInr(),
                            priceResult.getGstPercentageApplied(),
                            priceResult.getGstAmountInr(),
                            priceResult.getTotalPayableInr(),
                            priceResult.getGstVerificationStatus()
                    );
                    planDtos.add(planDto);
                }
            }

            dto.setRatePlans(planDtos);
            categoryDtos.add(dto);
        }

        return new AvailabilityResponse(checkIn, checkOut, nights, rooms, adults, children, categoryDtos);
    }

    public PricingQuoteDto calculatePricingQuote(
            Long roomCategoryId,
            String categoryCode,
            Long ratePlanId,
            String planCode,
            LocalDate checkIn,
            LocalDate checkOut,
            int rooms,
            int adults,
            int children,
            String promoCode
    ) {
        transactionService.validateStayDates(checkIn, checkOut);

        if (rooms < 1) {
            throw new IllegalArgumentException("Rooms must be at least 1");
        }
        if (adults < 1) {
            throw new IllegalArgumentException("Adults must be at least 1");
        }
        if (children < 0) {
            throw new IllegalArgumentException("Children cannot be negative");
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);

        RoomCategory category;
        if (roomCategoryId != null) {
            category = roomCategoryRepository.findById(roomCategoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room category not found with id: " + roomCategoryId));
        } else if (categoryCode != null && !categoryCode.trim().isEmpty()) {
            category = roomCategoryRepository.findByCode(categoryCode.trim().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Room category not found with code: " + categoryCode));
        } else {
            throw new IllegalArgumentException("Room category id or code is required");
        }

        RatePlan ratePlan;
        if (ratePlanId != null) {
            ratePlan = ratePlanRepository.findByIdAndIsActiveTrue(ratePlanId)
                    .orElseThrow(() -> new ResourceNotFoundException("Active rate plan not found with id: " + ratePlanId));
        } else if (planCode != null && !planCode.trim().isEmpty()) {
            ratePlan = ratePlanRepository.findByRoomCategoryIdAndCodeIgnoreCaseAndIsActiveTrue(category.getId(), planCode.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Active rate plan not found with code: " + planCode));
        } else {
            throw new IllegalArgumentException("Rate plan id or code is required");
        }

        if (!ratePlan.getRoomCategory().getId().equals(category.getId())) {
            throw new IllegalArgumentException("Rate plan does not belong to the selected room category");
        }

        // Blackout check
        List<BlackoutDate> blackouts = blackoutDateRepository.findConflictingBlackouts(
                category.getId(), checkIn, checkOut);
        if (!blackouts.isEmpty()) {
            throw new BlackoutDateConflictException("Requested stay dates conflict with a resort blackout: " + blackouts.get(0).getReason());
        }

        // Daily inventory rates (read-only)
        List<DailyInventoryRate> rates = dailyInventoryRateRepository
                .findByRoomCategoryIdAndStayDateRange(category.getId(), checkIn, checkOut);

        if (rates.size() != nights) {
            throw new InsufficientAllotmentException("Inventory is not configured for all nights in the requested stay");
        }

        // Allotment headroom check
        for (DailyInventoryRate rate : rates) {
            if (rate.getBookedCount() + rooms > rate.getWebsiteAllotmentTotal()) {
                throw new InsufficientAllotmentException("Insufficient website allotment for date: " + rate.getStayDate());
            }
        }

        // Calculate authoritative pricing via existing PricingService
        PriceCalculationResult priceResult = pricingService.calculatePrice(rates, ratePlan, rooms, promoCode);

        // Summed nightly base rates * rooms
        BigDecimal baseSum = rates.stream()
                .map(DailyInventoryRate::getBaseRateInr)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(rooms))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal addonSum = priceResult.getSubtotalBeforeDiscountInr().subtract(baseSum)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // Itemized nightly breakdown
        List<NightlyTariffDto> nightlyBreakdown = new ArrayList<>();
        for (DailyInventoryRate rate : rates) {
            BigDecimal base = rate.getBaseRateInr();
            BigDecimal addon = BigDecimal.ZERO;
            if (ratePlan.getPriceModifierType() == PriceModifierType.FIXED_ADDON) {
                addon = ratePlan.getPriceModifierValue();
            } else if (ratePlan.getPriceModifierType() == PriceModifierType.PERCENTAGE) {
                addon = base.multiply(ratePlan.getPriceModifierValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            nightlyBreakdown.add(new NightlyTariffDto(
                    rate.getStayDate(),
                    base,
                    addon,
                    base.add(addon).setScale(2, RoundingMode.HALF_UP)
            ));
        }

        PricingQuoteDto dto = new PricingQuoteDto();
        dto.setRoomCategoryId(category.getId());
        dto.setRoomCategoryCode(category.getCode());
        dto.setRoomCategoryName(category.getName());
        dto.setRatePlanId(ratePlan.getId());
        dto.setRatePlanCode(ratePlan.getCode());
        dto.setRatePlanName(ratePlan.getName());
        dto.setCheckInDate(checkIn);
        dto.setCheckOutDate(checkOut);
        dto.setNights(nights);
        dto.setRoomsCount(rooms);
        dto.setAdults(adults);
        dto.setChildren(children);

        dto.setBaseAccommodationTotalInr(baseSum);
        dto.setRatePlanAddonTotalInr(addonSum);
        dto.setSubtotalBeforeDiscountInr(priceResult.getSubtotalBeforeDiscountInr());
        dto.setPromoCode(priceResult.getPromoCode() != null ? priceResult.getPromoCode().getCode() : null);
        dto.setDiscountAmountInr(priceResult.getDiscountAmountInr());
        dto.setDiscountedPreTaxInr(priceResult.getTaxableAmountInr());
        dto.setTaxableAmountInr(priceResult.getTaxableAmountInr());
        dto.setGstPercentageApplied(priceResult.getGstPercentageApplied());
        dto.setGstAmountInr(priceResult.getGstAmountInr());
        dto.setTotalPayableInr(priceResult.getTotalPayableInr());
        dto.setGstVerificationStatus(priceResult.getGstVerificationStatus());
        dto.setNightlyRates(nightlyBreakdown);

        return dto;
    }

    public BookingResponseDto createBookingAndLockAllotment(CreateBookingRequest request, String idempotencyKeyHeader) {
        String idempotencyKey = (idempotencyKeyHeader != null && !idempotencyKeyHeader.trim().isEmpty())
                ? idempotencyKeyHeader.trim()
                : (request.getIdempotencyKey() != null ? request.getIdempotencyKey().trim() : null);

        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            throw new IllegalArgumentException("Idempotency-Key is required either as a header or in the request body");
        }

        // Pre-check idempotency key
        Optional<Booking> fastCheck = bookingRepository.findByIdempotencyKey(idempotencyKey);
        if (fastCheck.isPresent()) {
            transactionService.validatePayloadMatch(fastCheck.get(), request);
            Payment payment = paymentRepository.findByBookingId(fastCheck.get().getId()).orElse(null);
            return transactionService.mapToBookingResponseDto(fastCheck.get(), payment != null ? payment.getOneTimePaymentToken() : null);
        }

        int maxRetries = 3;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return transactionService.executeBookingTransaction(request, idempotencyKey);
            } catch (DataIntegrityViolationException ex) {
                // Idempotency race: another thread committed with the exact same idempotency_key
                for (int poll = 0; poll < 10; poll++) {
                    Optional<Booking> raced = bookingRepository.findByIdempotencyKey(idempotencyKey);
                    if (raced.isPresent()) {
                        transactionService.validatePayloadMatch(raced.get(), request);
                        Payment payment = paymentRepository.findByBookingId(raced.get().getId()).orElse(null);
                        return transactionService.mapToBookingResponseDto(raced.get(), payment != null ? payment.getOneTimePaymentToken() : null);
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                throw ex;
            } catch (TransientDataAccessException | LockAcquisitionException ex) {
                if (attempt == maxRetries) {
                    throw new ServiceUnavailableException("Database lock acquisition timeout or deadlock after retries; please try again", ex);
                }
                try {
                    Thread.sleep(50L * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ServiceUnavailableException("Booking request interrupted", ie);
                }
            }
        }

        throw new ServiceUnavailableException("Database lock acquisition timeout after retries");
    }

    private List<String> parsePhotoUrls(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
