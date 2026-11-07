package com.companya.resort.service;

import com.companya.resort.dto.AdminDailyInventoryRateDto;
import com.companya.resort.dto.BulkUpdateInventoryRatesRequest;
import com.companya.resort.entity.BlackoutDate;
import com.companya.resort.entity.DailyInventoryRate;
import com.companya.resort.entity.RoomCategory;
import com.companya.resort.exception.InvalidDateRangeException;
import com.companya.resort.exception.MissingInventoryDatesException;
import com.companya.resort.repository.BlackoutDateRepository;
import com.companya.resort.repository.DailyInventoryRateRepository;
import com.companya.resort.repository.RoomCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminInventoryService {

    private static final int MAX_RANGE_DAYS = 62;

    private final DailyInventoryRateRepository dailyInventoryRateRepository;
    private final RoomCategoryRepository roomCategoryRepository;
    private final BlackoutDateRepository blackoutDateRepository;

    public AdminInventoryService(
            DailyInventoryRateRepository dailyInventoryRateRepository,
            RoomCategoryRepository roomCategoryRepository,
            BlackoutDateRepository blackoutDateRepository
    ) {
        this.dailyInventoryRateRepository = dailyInventoryRateRepository;
        this.roomCategoryRepository = roomCategoryRepository;
        this.blackoutDateRepository = blackoutDateRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminDailyInventoryRateDto> getInventoryRates(LocalDate from, LocalDate to) {
        validateDateRange(from, to);

        List<DailyInventoryRate> rates = dailyInventoryRateRepository.findByStayDateBetweenInclusive(from, to);
        List<BlackoutDate> blackouts = blackoutDateRepository.findBlackoutsBetweenInclusive(from, to);

        return rates.stream()
                .map(rate -> toDto(rate, blackouts))
                .sorted(Comparator.comparing(AdminDailyInventoryRateDto::stayDate)
                        .thenComparing(AdminDailyInventoryRateDto::categoryCode))
                .toList();
    }

    @Transactional
    public List<AdminDailyInventoryRateDto> bulkUpdateInventoryRates(BulkUpdateInventoryRatesRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        String categoryCode = request.roomCategoryCode() != null ? request.roomCategoryCode().trim().toUpperCase() : "";
        if (categoryCode.isBlank()) {
            throw new IllegalArgumentException("Room category code is required");
        }

        RoomCategory category = roomCategoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid room category code: " + request.roomCategoryCode()));

        LocalDate from = request.from();
        LocalDate to = request.to();
        validateDateRange(from, to);

        BigDecimal baseRateInr = request.baseRateInr();
        if (baseRateInr == null || baseRateInr.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Base rate must be greater than zero");
        }
        if (baseRateInr.scale() > 2 && baseRateInr.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Base rate cannot have more than 2 decimal places");
        }

        Integer allotment = request.websiteAllotmentTotal();
        if (allotment == null || allotment < 0) {
            throw new IllegalArgumentException("Website allotment total cannot be negative");
        }
        if (allotment > category.getTotalPhysicalKeys()) {
            throw new IllegalArgumentException("Website allotment total (" + allotment +
                    ") cannot exceed total physical keys (" + category.getTotalPhysicalKeys() +
                    ") for category " + category.getCode());
        }

        List<DailyInventoryRate> existingRates =
                dailyInventoryRateRepository.findByRoomCategoryCodeAndStayDateBetweenInclusive(category.getCode(), from, to);

        Set<LocalDate> presentDates = existingRates.stream()
                .map(DailyInventoryRate::getStayDate)
                .collect(Collectors.toSet());

        List<LocalDate> missingDates = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (!presentDates.contains(d)) {
                missingDates.add(d);
            }
        }

        if (!missingDates.isEmpty()) {
            throw new MissingInventoryDatesException(
                    "Missing inventory rate records for dates: " + missingDates,
                    missingDates
            );
        }

        for (DailyInventoryRate rate : existingRates) {
            if (allotment < rate.getBookedCount()) {
                throw new IllegalArgumentException(
                        "Website allotment total (" + allotment +
                                ") cannot be less than booked count (" + rate.getBookedCount() +
                                ") on " + rate.getStayDate()
                );
            }
        }

        for (DailyInventoryRate rate : existingRates) {
            rate.setBaseRateInr(baseRateInr);
            rate.setWebsiteAllotmentTotal(allotment);
            // Never update booked_count or version manually. Let JPA @Version increment.
        }

        dailyInventoryRateRepository.saveAll(existingRates);
        dailyInventoryRateRepository.flush();

        List<BlackoutDate> blackouts = blackoutDateRepository.findBlackoutsBetweenInclusive(from, to);

        return existingRates.stream()
                .map(rate -> toDto(rate, blackouts))
                .sorted(Comparator.comparing(AdminDailyInventoryRateDto::stayDate))
                .toList();
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new InvalidDateRangeException("Both 'from' and 'to' dates must be provided");
        }
        if (from.isAfter(to)) {
            throw new InvalidDateRangeException("Invalid date range: 'from' must be on or before 'to'");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new InvalidDateRangeException("Date range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
    }

    private AdminDailyInventoryRateDto toDto(DailyInventoryRate rate, List<BlackoutDate> blackouts) {
        BlackoutDate matchingBlackout = blackouts.stream()
                .filter(b -> b.getBlackoutDate().equals(rate.getStayDate()) &&
                        (b.getRoomCategory() == null || b.getRoomCategory().getId().equals(rate.getRoomCategory().getId())))
                .findFirst()
                .orElse(null);

        boolean isBlackout = matchingBlackout != null;
        String blackoutReason = isBlackout ? matchingBlackout.getReason() : null;

        return new AdminDailyInventoryRateDto(
                rate.getId(),
                rate.getRoomCategory().getId(),
                rate.getRoomCategory().getCode(),
                rate.getRoomCategory().getName(),
                rate.getStayDate(),
                rate.getBaseRateInr(),
                rate.getWebsiteAllotmentTotal(),
                rate.getBookedCount(),
                rate.getRoomCategory().getTotalPhysicalKeys(),
                rate.getVersion(),
                isBlackout,
                blackoutReason
        );
    }
}
