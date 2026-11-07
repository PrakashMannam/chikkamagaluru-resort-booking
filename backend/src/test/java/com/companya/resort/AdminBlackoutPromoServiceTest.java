package com.companya.resort;

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
import com.companya.resort.service.AdminBlackoutPromoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBlackoutPromoServiceTest {

    @Mock
    private BlackoutDateRepository blackoutDateRepository;

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @Mock
    private RoomCategoryRepository roomCategoryRepository;

    @InjectMocks
    private AdminBlackoutPromoService service;

    private RoomCategory roomCategory;
    private RoomCategory villaCategory;

    @BeforeEach
    void setUp() {
        roomCategory = new RoomCategory(1L, "ROOM", "Estate Room", 64, 2, 1, "Desc", "[]");
        villaCategory = new RoomCategory(2L, "VILLA", "Private Villa", 12, 4, 2, "Desc", "[]");
    }

    // ==========================================
    // BLACKOUT TESTS
    // ==========================================

    @Test
    @DisplayName("Create resort-wide blackout succeeds")
    void testCreateResortWideBlackoutSuccess() {
        LocalDate date = LocalDate.of(2026, 12, 31);
        CreateBlackoutRequest req = new CreateBlackoutRequest(null, null, date, "New Year Buyout");

        when(blackoutDateRepository.existsByRoomCategoryIsNullAndBlackoutDate(date)).thenReturn(false);
        when(blackoutDateRepository.saveAndFlush(any(BlackoutDate.class))).thenAnswer(invocation -> {
            BlackoutDate b = invocation.getArgument(0);
            return new BlackoutDate(100L, b.getRoomCategory(), b.getBlackoutDate(), b.getReason());
        });

        AdminBlackoutDto dto = service.createBlackout(req);

        assertNotNull(dto);
        assertEquals(100L, dto.id());
        assertNull(dto.roomCategoryId());
        assertNull(dto.roomCategoryCode());
        assertEquals("All Resort", dto.roomCategoryName());
        assertEquals(date, dto.blackoutDate());
        assertEquals("New Year Buyout", dto.reason());
        verify(blackoutDateRepository, times(1)).saveAndFlush(any(BlackoutDate.class));
    }

    @Test
    @DisplayName("Create category-specific blackout succeeds")
    void testCreateCategorySpecificBlackoutSuccess() {
        LocalDate date = LocalDate.of(2026, 11, 20);
        CreateBlackoutRequest req = new CreateBlackoutRequest("VILLA", null, date, "Private Pool Villa Maintenance");

        when(roomCategoryRepository.findByCode("VILLA")).thenReturn(Optional.of(villaCategory));
        when(blackoutDateRepository.existsByRoomCategoryIsNullAndBlackoutDate(date)).thenReturn(false);
        when(blackoutDateRepository.existsByRoomCategoryIdAndBlackoutDate(villaCategory.getId(), date)).thenReturn(false);
        when(blackoutDateRepository.saveAndFlush(any(BlackoutDate.class))).thenAnswer(invocation -> {
            BlackoutDate b = invocation.getArgument(0);
            return new BlackoutDate(101L, b.getRoomCategory(), b.getBlackoutDate(), b.getReason());
        });

        AdminBlackoutDto dto = service.createBlackout(req);

        assertNotNull(dto);
        assertEquals(101L, dto.id());
        assertEquals(2L, dto.roomCategoryId());
        assertEquals("VILLA", dto.roomCategoryCode());
        assertEquals("Private Villa", dto.roomCategoryName());
        assertEquals(date, dto.blackoutDate());
        verify(blackoutDateRepository, times(1)).saveAndFlush(any(BlackoutDate.class));
    }

    @Test
    @DisplayName("Create blackout handles DB duplicate key race on resort-wide blackout and throws BlackoutDateConflictException")
    void testCreateResortWideBlackoutDbRaceThrowsConflictException() {
        LocalDate date = LocalDate.of(2026, 12, 31);
        CreateBlackoutRequest req = new CreateBlackoutRequest(null, null, date, "New Year Buyout");

        when(blackoutDateRepository.existsByRoomCategoryIsNullAndBlackoutDate(date)).thenReturn(false);
        when(blackoutDateRepository.saveAndFlush(any(BlackoutDate.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry '0-2026-12-31' for key 'uk_blackout_dates_scope_date'"));

        BlackoutDateConflictException ex = assertThrows(BlackoutDateConflictException.class, () -> service.createBlackout(req));
        assertTrue(ex.getMessage().contains("A resort-wide blackout date already exists on 2026-12-31"));
    }

    @Test
    @DisplayName("Create blackout handles DB duplicate key race on category blackout and throws BlackoutDateConflictException")
    void testCreateCategoryBlackoutDbRaceThrowsConflictException() {
        LocalDate date = LocalDate.of(2026, 11, 20);
        CreateBlackoutRequest req = new CreateBlackoutRequest("VILLA", null, date, "Villa Maintenance");

        when(roomCategoryRepository.findByCode("VILLA")).thenReturn(Optional.of(villaCategory));
        when(blackoutDateRepository.existsByRoomCategoryIsNullAndBlackoutDate(date)).thenReturn(false);
        when(blackoutDateRepository.existsByRoomCategoryIdAndBlackoutDate(villaCategory.getId(), date)).thenReturn(false);
        when(blackoutDateRepository.saveAndFlush(any(BlackoutDate.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry '2-2026-11-20' for key 'uk_blackout_dates_scope_date'"));

        BlackoutDateConflictException ex = assertThrows(BlackoutDateConflictException.class, () -> service.createBlackout(req));
        assertTrue(ex.getMessage().contains("A blackout date for category VILLA already exists on 2026-11-20"));
    }

    @Test
    @DisplayName("Create blackout re-throws DataIntegrityViolationException when error is not a blackout unique constraint violation")
    void testCreateBlackoutDbUnrelatedIntegrityViolationRethrows() {
        LocalDate date = LocalDate.of(2026, 12, 31);
        CreateBlackoutRequest req = new CreateBlackoutRequest(null, null, date, "New Year Buyout");

        when(blackoutDateRepository.existsByRoomCategoryIsNullAndBlackoutDate(date)).thenReturn(false);
        when(blackoutDateRepository.saveAndFlush(any(BlackoutDate.class)))
                .thenThrow(new DataIntegrityViolationException("Cannot add or update a child row: a foreign key constraint fails"));

        assertThrows(DataIntegrityViolationException.class, () -> service.createBlackout(req));
    }

    @Test
    @DisplayName("Create blackout rejects duplicate resort-wide blackout with 409 BlackoutDateConflictException")
    void testCreateBlackoutRejectsDuplicateResortWide() {
        LocalDate date = LocalDate.of(2026, 12, 25);
        CreateBlackoutRequest req = new CreateBlackoutRequest(null, null, date, "Christmas Buyout");

        when(blackoutDateRepository.existsByRoomCategoryIsNullAndBlackoutDate(date)).thenReturn(true);

        assertThrows(BlackoutDateConflictException.class, () -> service.createBlackout(req));
        verify(blackoutDateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create blackout rejects invalid category code with IllegalArgumentException")
    void testCreateBlackoutRejectsInvalidCategory() {
        LocalDate date = LocalDate.of(2026, 11, 25);
        CreateBlackoutRequest req = new CreateBlackoutRequest("INVALID_CAT", null, date, "Reason");

        when(roomCategoryRepository.findByCode("INVALID_CAT")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.createBlackout(req));
        verify(blackoutDateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get blackouts filters by date range and returns list ordered by date")
    void testGetBlackoutsByRange() {
        LocalDate d1 = LocalDate.of(2026, 12, 24);
        LocalDate d2 = LocalDate.of(2026, 12, 25);

        BlackoutDate b1 = new BlackoutDate(1L, null, d1, "Christmas Eve");
        BlackoutDate b2 = new BlackoutDate(2L, roomCategory, d2, "Room Maintenance");

        when(blackoutDateRepository.findBlackoutsBetweenInclusiveOrdered(d1, d2)).thenReturn(List.of(b1, b2));

        List<AdminBlackoutDto> dtos = service.getBlackouts(d1, d2);

        assertEquals(2, dtos.size());
        assertEquals(d1, dtos.get(0).blackoutDate());
        assertNull(dtos.get(0).roomCategoryCode());
        assertEquals(d2, dtos.get(1).blackoutDate());
        assertEquals("ROOM", dtos.get(1).roomCategoryCode());
    }

    @Test
    @DisplayName("Delete blackout deletes existing record and throws ResourceNotFoundException when missing")
    void testDeleteBlackout() {
        BlackoutDate b = new BlackoutDate(50L, null, LocalDate.of(2026, 11, 30), "Test");
        when(blackoutDateRepository.findById(50L)).thenReturn(Optional.of(b));

        service.deleteBlackout(50L);
        verify(blackoutDateRepository, times(1)).delete(b);

        when(blackoutDateRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteBlackout(999L));
    }

    // ==========================================
    // PROMO TESTS
    // ==========================================

    @Test
    @DisplayName("Create promo with PERCENTAGE discount succeeds")
    void testCreatePromoPercentageSuccess() {
        CreatePromoRequest req = new CreatePromoRequest(
                "WINTER20",
                DiscountType.PERCENTAGE,
                new BigDecimal("20.00"),
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2027, 1, 31),
                2,
                500,
                true
        );

        when(promoCodeRepository.existsByCodeIgnoreCase("WINTER20")).thenReturn(false);
        when(promoCodeRepository.save(any(PromoCode.class))).thenAnswer(invocation -> {
            PromoCode p = invocation.getArgument(0);
            return new PromoCode(10L, p.getCode(), p.getDiscountType(), p.getDiscountValue(),
                    p.getValidFrom(), p.getValidTo(), p.getMinNights(), p.getMaxUses(), p.getCurrentUses(), p.getIsActive());
        });

        AdminPromoDto dto = service.createPromo(req);

        assertNotNull(dto);
        assertEquals(10L, dto.id());
        assertEquals("WINTER20", dto.code());
        assertEquals(DiscountType.PERCENTAGE, dto.discountType());
        assertEquals(new BigDecimal("20.00"), dto.discountValue());
        assertEquals(0, dto.currentUses());
        assertTrue(dto.isActive());
        verify(promoCodeRepository, times(1)).save(any(PromoCode.class));
    }

    @Test
    @DisplayName("Create promo rejects duplicate promo code")
    void testCreatePromoRejectsDuplicate() {
        CreatePromoRequest req = new CreatePromoRequest(
                "DIRECT10",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2027, 4, 30),
                1,
                100,
                true
        );

        when(promoCodeRepository.existsByCodeIgnoreCase("DIRECT10")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createPromo(req));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(promoCodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create promo rejects percentage discount greater than 100")
    void testCreatePromoRejectsPercentageGreaterThan100() {
        CreatePromoRequest req = new CreatePromoRequest(
                "INVALID150",
                DiscountType.PERCENTAGE,
                new BigDecimal("150.00"),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2027, 4, 30),
                1,
                100,
                true
        );

        when(promoCodeRepository.existsByCodeIgnoreCase("INVALID150")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createPromo(req));
        assertTrue(ex.getMessage().contains("cannot exceed 100%"));
    }

    @Test
    @DisplayName("Create promo rejects inverted validity dates")
    void testCreatePromoRejectsInvertedDates() {
        CreatePromoRequest req = new CreatePromoRequest(
                "INVERTED",
                DiscountType.FLAT_INR,
                new BigDecimal("1000.00"),
                LocalDate.of(2027, 4, 30),
                LocalDate.of(2026, 11, 1),
                1,
                100,
                true
        );

        when(promoCodeRepository.existsByCodeIgnoreCase("INVERTED")).thenReturn(false);

        assertThrows(InvalidDateRangeException.class, () -> service.createPromo(req));
    }

    @Test
    @DisplayName("Update promo active toggles isActive status safely")
    void testUpdatePromoActiveToggle() {
        PromoCode promo = new PromoCode(
                5L, "FESTIVE15", DiscountType.PERCENTAGE, new BigDecimal("15.00"),
                LocalDate.of(2026, 11, 1), LocalDate.of(2027, 4, 30), 2, 200, 10, true
        );

        when(promoCodeRepository.findById(5L)).thenReturn(Optional.of(promo));
        when(promoCodeRepository.save(any(PromoCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminPromoDto updated = service.updatePromoActive(5L, new UpdatePromoActiveRequest(false));

        assertFalse(updated.isActive());
        assertEquals(5L, updated.id());
        assertEquals("FESTIVE15", updated.code());
        assertEquals(10, updated.currentUses()); // preserves currentUses!
        verify(promoCodeRepository, times(1)).save(promo);
    }
}
