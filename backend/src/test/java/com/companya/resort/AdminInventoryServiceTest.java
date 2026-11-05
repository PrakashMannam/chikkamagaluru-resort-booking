package com.companya.resort;

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
import com.companya.resort.service.AdminInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInventoryServiceTest {

    @Mock
    private DailyInventoryRateRepository dailyInventoryRateRepository;

    @Mock
    private RoomCategoryRepository roomCategoryRepository;

    @Mock
    private BlackoutDateRepository blackoutDateRepository;

    @InjectMocks
    private AdminInventoryService adminInventoryService;

    private RoomCategory roomCategory;
    private RoomCategory villaCategory;

    @BeforeEach
    void setUp() {
        roomCategory = new RoomCategory(1L, "ROOM", "Estate Room", 64, 2, 1, "Desc", "[]");
        villaCategory = new RoomCategory(2L, "VILLA", "Private Villa", 12, 4, 2, "Desc", "[]");
    }

    @Test
    @DisplayName("Bulk update rejects allotment below booked_count and leaves every row unchanged")
    void testBulkUpdateRejectsAllotmentBelowBookedCountAndLeavesRowsUnchanged() {
        when(roomCategoryRepository.findByCode("ROOM")).thenReturn(Optional.of(roomCategory));

        LocalDate date1 = LocalDate.of(2026, 11, 10);
        LocalDate date2 = LocalDate.of(2026, 11, 11);

        BigDecimal initialRate = new BigDecimal("8500.00");
        DailyInventoryRate row1 = new DailyInventoryRate(101L, roomCategory, date1, initialRate, 20, 5, 0L);
        DailyInventoryRate row2 = new DailyInventoryRate(102L, roomCategory, date2, initialRate, 20, 12, 0L);

        when(dailyInventoryRateRepository.findByRoomCategoryCodeAndStayDateBetweenInclusive("ROOM", date1, date2))
                .thenReturn(List.of(row1, row2));

        // Attempting to set allotment to 10, but row2 has bookedCount = 12
        BulkUpdateInventoryRatesRequest request = new BulkUpdateInventoryRatesRequest(
                "ROOM",
                date1,
                date2,
                new BigDecimal("9900.00"),
                10
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adminInventoryService.bulkUpdateInventoryRates(request)
        );

        assertTrue(ex.getMessage().contains("cannot be less than booked count (12)"));
        verify(dailyInventoryRateRepository, never()).saveAll(any());
        verify(dailyInventoryRateRepository, never()).flush();

        // Ensure rows remain unchanged
        assertEquals(initialRate, row1.getBaseRateInr());
        assertEquals(20, row1.getWebsiteAllotmentTotal());
        assertEquals(5, row1.getBookedCount());

        assertEquals(initialRate, row2.getBaseRateInr());
        assertEquals(20, row2.getWebsiteAllotmentTotal());
        assertEquals(12, row2.getBookedCount());
    }

    @Test
    @DisplayName("Bulk update rejects allotment above total_physical_keys and leaves every row unchanged")
    void testBulkUpdateRejectsAllotmentAboveTotalPhysicalKeysAndLeavesRowsUnchanged() {
        when(roomCategoryRepository.findByCode("VILLA")).thenReturn(Optional.of(villaCategory));

        LocalDate date1 = LocalDate.of(2026, 11, 10);
        LocalDate date2 = LocalDate.of(2026, 11, 11);

        // VILLA total_physical_keys is 12. Attempting to set allotment to 15.
        BulkUpdateInventoryRatesRequest request = new BulkUpdateInventoryRatesRequest(
                "VILLA",
                date1,
                date2,
                new BigDecimal("15000.00"),
                15
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adminInventoryService.bulkUpdateInventoryRates(request)
        );

        assertTrue(ex.getMessage().contains("cannot exceed total physical keys (12)"));
        verify(dailyInventoryRateRepository, never()).findByRoomCategoryCodeAndStayDateBetweenInclusive(any(), any(), any());
        verify(dailyInventoryRateRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Bulk update rejects missing date in range and leaves neighboring rows unchanged")
    void testBulkUpdateRejectsMissingDateInRangeAndLeavesNeighboringRowsUnchanged() {
        when(roomCategoryRepository.findByCode("ROOM")).thenReturn(Optional.of(roomCategory));

        LocalDate date1 = LocalDate.of(2026, 11, 10);
        LocalDate date2 = LocalDate.of(2026, 11, 11); // missing in DB
        LocalDate date3 = LocalDate.of(2026, 11, 12);

        BigDecimal initialRate = new BigDecimal("8500.00");
        DailyInventoryRate row1 = new DailyInventoryRate(101L, roomCategory, date1, initialRate, 20, 2, 0L);
        DailyInventoryRate row3 = new DailyInventoryRate(103L, roomCategory, date3, initialRate, 20, 3, 0L);

        // DB only returns row1 and row3 (date2 is missing)
        when(dailyInventoryRateRepository.findByRoomCategoryCodeAndStayDateBetweenInclusive("ROOM", date1, date3))
                .thenReturn(List.of(row1, row3));

        BulkUpdateInventoryRatesRequest request = new BulkUpdateInventoryRatesRequest(
                "ROOM",
                date1,
                date3,
                new BigDecimal("9900.00"),
                25
        );

        MissingInventoryDatesException ex = assertThrows(MissingInventoryDatesException.class, () ->
                adminInventoryService.bulkUpdateInventoryRates(request)
        );

        assertEquals(List.of(date2), ex.getMissingDates());
        verify(dailyInventoryRateRepository, never()).saveAll(any());
        verify(dailyInventoryRateRepository, never()).flush();

        // Verify neighboring rows were untouched
        assertEquals(initialRate, row1.getBaseRateInr());
        assertEquals(20, row1.getWebsiteAllotmentTotal());
        assertEquals(2, row1.getBookedCount());

        assertEquals(initialRate, row3.getBaseRateInr());
        assertEquals(20, row3.getWebsiteAllotmentTotal());
        assertEquals(3, row3.getBookedCount());
    }

    @Test
    @DisplayName("Bulk update changes rate and allotment and does not change booked_count")
    void testBulkUpdateChangesRateAndAllotmentAndDoesNotChangeBookedCount() {
        when(roomCategoryRepository.findByCode("ROOM")).thenReturn(Optional.of(roomCategory));

        LocalDate date1 = LocalDate.of(2026, 11, 10);
        LocalDate date2 = LocalDate.of(2026, 11, 11);

        BigDecimal oldRate = new BigDecimal("8500.00");
        BigDecimal newRate = new BigDecimal("9800.50");
        int newAllotment = 30;

        DailyInventoryRate row1 = new DailyInventoryRate(101L, roomCategory, date1, oldRate, 20, 4, 1L);
        DailyInventoryRate row2 = new DailyInventoryRate(102L, roomCategory, date2, oldRate, 20, 7, 2L);

        when(dailyInventoryRateRepository.findByRoomCategoryCodeAndStayDateBetweenInclusive("ROOM", date1, date2))
                .thenReturn(List.of(row1, row2));
        when(blackoutDateRepository.findBlackoutsBetweenInclusive(date1, date2))
                .thenReturn(List.of());

        BulkUpdateInventoryRatesRequest request = new BulkUpdateInventoryRatesRequest(
                "ROOM",
                date1,
                date2,
                newRate,
                newAllotment
        );

        List<AdminDailyInventoryRateDto> result = adminInventoryService.bulkUpdateInventoryRates(request);

        assertEquals(2, result.size());

        // Verify row1
        assertEquals(newRate, row1.getBaseRateInr());
        assertEquals(newAllotment, row1.getWebsiteAllotmentTotal());
        assertEquals(4, row1.getBookedCount()); // Unchanged!

        // Verify row2
        assertEquals(newRate, row2.getBaseRateInr());
        assertEquals(newAllotment, row2.getWebsiteAllotmentTotal());
        assertEquals(7, row2.getBookedCount()); // Unchanged!

        verify(dailyInventoryRateRepository, times(1)).saveAll(List.of(row1, row2));
        verify(dailyInventoryRateRepository, times(1)).flush();

        // Verify DTO results
        assertEquals(date1, result.get(0).stayDate());
        assertEquals(newRate, result.get(0).baseRateInr());
        assertEquals(newAllotment, result.get(0).websiteAllotmentTotal());
        assertEquals(4, result.get(0).bookedCount());
        assertFalse(result.get(0).blackout());

        assertEquals(date2, result.get(1).stayDate());
        assertEquals(newRate, result.get(1).baseRateInr());
        assertEquals(newAllotment, result.get(1).websiteAllotmentTotal());
        assertEquals(7, result.get(1).bookedCount());
        assertFalse(result.get(1).blackout());
    }

    @Test
    @DisplayName("Range validations reject inverted range and range > 62 days")
    void testDateRangeValidations() {
        LocalDate start = LocalDate.of(2026, 11, 10);
        LocalDate beforeStart = LocalDate.of(2026, 11, 9);
        LocalDate farEnd = start.plusDays(65);

        assertThrows(InvalidDateRangeException.class, () ->
                adminInventoryService.getInventoryRates(start, beforeStart)
        );

        assertThrows(InvalidDateRangeException.class, () ->
                adminInventoryService.getInventoryRates(start, farEnd)
        );
    }

    @Test
    @DisplayName("GET inventory rates returns both categories with joined blackout flags")
    void testGetInventoryRatesWithBlackout() {
        LocalDate date = LocalDate.of(2026, 12, 24);

        DailyInventoryRate roomRate = new DailyInventoryRate(1L, roomCategory, date, new BigDecimal("10000.00"), 40, 2, 0L);
        DailyInventoryRate villaRate = new DailyInventoryRate(2L, villaCategory, date, new BigDecimal("25000.00"), 10, 1, 0L);

        when(dailyInventoryRateRepository.findByStayDateBetweenInclusive(date, date))
                .thenReturn(List.of(roomRate, villaRate));

        // Resort-wide blackout (roomCategory is null)
        BlackoutDate resortBlackout = new BlackoutDate(1L, null, date, "Christmas Buyout");
        when(blackoutDateRepository.findBlackoutsBetweenInclusive(date, date))
                .thenReturn(List.of(resortBlackout));

        List<AdminDailyInventoryRateDto> dtos = adminInventoryService.getInventoryRates(date, date);

        assertEquals(2, dtos.size());
        assertTrue(dtos.get(0).blackout());
        assertEquals("Christmas Buyout", dtos.get(0).blackoutReason());
        assertTrue(dtos.get(1).blackout());
        assertEquals("Christmas Buyout", dtos.get(1).blackoutReason());
    }
}
