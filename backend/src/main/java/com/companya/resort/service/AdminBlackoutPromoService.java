package com.companya.resort.service;

import com.companya.resort.dto.*;
import com.companya.resort.entity.BlackoutDate;
import com.companya.resort.entity.DiscountType;
import com.companya.resort.entity.PromoCode;
import com.companya.resort.entity.RoomCategory;
import com.companya.resort.exception.BlackoutDateConflictException;
import com.companya.resort.exception.InvalidDateRangeException;
import com.companya.resort.exception.ResourceNotFoundException;
import com.companya.resort.repository.BlackoutDateRepository;
import com.companya.resort.repository.PromoCodeRepository;
import com.companya.resort.repository.RoomCategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AdminBlackoutPromoService {

    private final BlackoutDateRepository blackoutDateRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final RoomCategoryRepository roomCategoryRepository;

    public AdminBlackoutPromoService(
            BlackoutDateRepository blackoutDateRepository,
            PromoCodeRepository promoCodeRepository,
            RoomCategoryRepository roomCategoryRepository
    ) {
        this.blackoutDateRepository = blackoutDateRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.roomCategoryRepository = roomCategoryRepository;
    }

    // ==========================================
    // BLACKOUT OPERATIONS
    // ==========================================

    @Transactional(readOnly = true)
    public List<AdminBlackoutDto> getBlackouts(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new InvalidDateRangeException("Invalid date range: 'from' must be on or before 'to'");
            }
            return blackoutDateRepository.findBlackoutsBetweenInclusiveOrdered(from, to)
                    .stream()
                    .map(this::toBlackoutDto)
                    .toList();
        }
        return blackoutDateRepository.findAllWithCategoryOrdered()
                .stream()
                .map(this::toBlackoutDto)
                .toList();
    }

    @Transactional
    public AdminBlackoutDto createBlackout(CreateBlackoutRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        LocalDate date = request.blackoutDate();
        if (date == null) {
            throw new IllegalArgumentException("Blackout date is required");
        }

        RoomCategory category = null;
        if (request.roomCategoryCode() != null && !request.roomCategoryCode().isBlank()) {
            String code = request.roomCategoryCode().trim().toUpperCase();
            category = roomCategoryRepository.findByCode(code)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid room category code: " + request.roomCategoryCode()));
        } else if (request.roomCategoryId() != null) {
            category = roomCategoryRepository.findById(request.roomCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid room category id: " + request.roomCategoryId()));
        }

        // Duplicate checks
        if (category == null) {
            if (blackoutDateRepository.existsByRoomCategoryIsNullAndBlackoutDate(date)) {
                throw new BlackoutDateConflictException("A resort-wide blackout date already exists on " + date);
            }
        } else {
            if (blackoutDateRepository.existsByRoomCategoryIsNullAndBlackoutDate(date)) {
                throw new BlackoutDateConflictException("A resort-wide blackout date already covers " + date);
            }
            if (blackoutDateRepository.existsByRoomCategoryIdAndBlackoutDate(category.getId(), date)) {
                throw new BlackoutDateConflictException("A blackout date for category " + category.getCode() + " already exists on " + date);
            }
        }

        String reason = request.reason() != null && !request.reason().isBlank()
                ? request.reason().trim()
                : "Resort Blackout";
        if (reason.length() > 255) {
            reason = reason.substring(0, 255);
        }

        BlackoutDate blackout = new BlackoutDate(null, category, date, reason);
        BlackoutDate saved = blackoutDateRepository.save(blackout);

        return toBlackoutDto(saved);
    }

    @Transactional
    public void deleteBlackout(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Blackout id is required");
        }
        BlackoutDate blackout = blackoutDateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blackout date not found with id: " + id));
        blackoutDateRepository.delete(blackout);
    }

    // ==========================================
    // PROMO OPERATIONS
    // ==========================================

    @Transactional(readOnly = true)
    public List<AdminPromoDto> getAllPromos() {
        return promoCodeRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(this::toPromoDto)
                .toList();
    }

    @Transactional
    public AdminPromoDto createPromo(CreatePromoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        String code = request.code() != null ? request.code().trim().toUpperCase() : "";
        if (code.isBlank()) {
            throw new IllegalArgumentException("Promo code is required");
        }
        if (code.length() > 50) {
            throw new IllegalArgumentException("Promo code cannot exceed 50 characters");
        }
        if (promoCodeRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Promo code '" + code + "' already exists");
        }

        DiscountType discountType = request.discountType();
        if (discountType == null) {
            throw new IllegalArgumentException("Discount type is required");
        }

        BigDecimal discountValue = request.discountValue();
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount value must be greater than zero");
        }
        if (discountValue.scale() > 2 && discountValue.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Discount value cannot have more than 2 decimal places");
        }
        if (discountType == DiscountType.PERCENTAGE && discountValue.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
        }

        LocalDate validFrom = request.validFrom();
        LocalDate validTo = request.validTo();
        if (validFrom == null || validTo == null) {
            throw new InvalidDateRangeException("Both 'validFrom' and 'validTo' dates are required");
        }
        if (validFrom.isAfter(validTo)) {
            throw new InvalidDateRangeException("validFrom must be on or before validTo");
        }

        Integer minNights = request.minNights() != null ? request.minNights() : 1;
        if (minNights < 1) {
            throw new IllegalArgumentException("minNights must be at least 1");
        }

        Integer maxUses = request.maxUses();
        if (maxUses == null || maxUses < 1) {
            throw new IllegalArgumentException("maxUses must be at least 1");
        }

        boolean isActive = request.isActive() == null || Boolean.TRUE.equals(request.isActive());

        PromoCode promo = new PromoCode(
                null,
                code,
                discountType,
                discountValue,
                validFrom,
                validTo,
                minNights,
                maxUses,
                0,
                isActive
        );

        PromoCode saved = promoCodeRepository.save(promo);
        return toPromoDto(saved);
    }

    @Transactional
    public AdminPromoDto updatePromoActive(Long id, UpdatePromoActiveRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("Promo id is required");
        }
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found with id: " + id));

        boolean active = request != null && request.resolveActive();
        promo.setIsActive(active);
        PromoCode saved = promoCodeRepository.save(promo);

        return toPromoDto(saved);
    }

    // ==========================================
    // MAPPERS
    // ==========================================

    private AdminBlackoutDto toBlackoutDto(BlackoutDate b) {
        Long catId = b.getRoomCategory() != null ? b.getRoomCategory().getId() : null;
        String catCode = b.getRoomCategory() != null ? b.getRoomCategory().getCode() : null;
        String catName = b.getRoomCategory() != null ? b.getRoomCategory().getName() : "All Resort";

        return new AdminBlackoutDto(
                b.getId(),
                catId,
                catCode,
                catName,
                b.getBlackoutDate(),
                b.getReason()
        );
    }

    private AdminPromoDto toPromoDto(PromoCode p) {
        return new AdminPromoDto(
                p.getId(),
                p.getCode(),
                p.getDiscountType(),
                p.getDiscountValue(),
                p.getValidFrom(),
                p.getValidTo(),
                p.getMinNights(),
                p.getMaxUses(),
                p.getCurrentUses(),
                p.getIsActive()
        );
    }
}
