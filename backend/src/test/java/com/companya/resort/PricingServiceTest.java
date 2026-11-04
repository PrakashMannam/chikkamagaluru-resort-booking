package com.companya.resort;

import com.companya.resort.dto.PriceCalculationResult;
import com.companya.resort.entity.*;
import com.companya.resort.repository.GstTaxSlabRepository;
import com.companya.resort.repository.PromoCodeRepository;
import com.companya.resort.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private GstTaxSlabRepository gstTaxSlabRepository;

    @Mock
    private PromoCodeRepository promoCodeRepository;

    private Clock fixedClock;
    private PricingService pricingService;

    private RoomCategory roomCategory;
    private RoomCategory villaCategory;
    private RatePlan planCp;
    private RatePlan planEp;

    // test values, not real GST rates
    private final GstTaxSlab TEST_ONLY_SLAB_5 = new GstTaxSlab(1L, new BigDecimal("0.00"), new BigDecimal("7500.00"), new BigDecimal("5.00"),
            LocalDate.of(2025, 9, 22), null, GstVerificationStatus.UNVERIFIED);
    private final GstTaxSlab TEST_ONLY_SLAB_18 = new GstTaxSlab(2L, new BigDecimal("7500.00"), null, new BigDecimal("18.00"),
            LocalDate.of(2025, 9, 22), null, GstVerificationStatus.UNVERIFIED);

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
                LocalDate.of(2026, 11, 2).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
        pricingService = new PricingService(gstTaxSlabRepository, promoCodeRepository, fixedClock);

        roomCategory = new RoomCategory(1L, "ROOM", "Coffee Estate Canopy Room", 64, 2, 2, "Room desc", "[]");
        villaCategory = new RoomCategory(2L, "VILLA", "Private Plantation Pool Villa", 12, 3, 2, "Villa desc", "[]");
        planCp = new RatePlan(2L, villaCategory, "CP", "Continental Plan", PriceModifierType.FIXED_ADDON, new BigDecimal("1200.00"), true);
        planEp = new RatePlan(1L, roomCategory, "EP", "European Plan", PriceModifierType.FIXED_ADDON, BigDecimal.ZERO, true);
    }

    @Test
    void average7500Selects5Percent_750001Selects18Percent_7500004Selects18Percent() {
        LocalDate checkInDate = LocalDate.of(2026, 11, 14);

        // Case 1: 1 night at 7,500.00 -> average is exactly 7500.0000
        DailyInventoryRate r7500 = new DailyInventoryRate(1L, roomCategory, checkInDate, new BigDecimal("7500.00"), 5, 0, 0L);
        when(gstTaxSlabRepository.findMatchingSlabs(eq(checkInDate), eq(new BigDecimal("7500.0000"))))
                .thenReturn(List.of(TEST_ONLY_SLAB_5));

        PriceCalculationResult result7500 = pricingService.calculatePrice(List.of(r7500), planEp, 1, null);
        assertEquals(new BigDecimal("5.00"), result7500.getGstPercentageApplied(), "Average 7,500.00 must select 5% slab");

        // Case 2: 1 night at 7,500.01 -> average is 7500.0100
        DailyInventoryRate r7501 = new DailyInventoryRate(2L, roomCategory, checkInDate, new BigDecimal("7500.01"), 5, 0, 0L);
        when(gstTaxSlabRepository.findMatchingSlabs(eq(checkInDate), eq(new BigDecimal("7500.0100"))))
                .thenReturn(List.of(TEST_ONLY_SLAB_18));

        PriceCalculationResult result7501 = pricingService.calculatePrice(List.of(r7501), planEp, 1, null);
        assertEquals(new BigDecimal("18.00"), result7501.getGstPercentageApplied(), "Average 7,500.01 must select 18% slab");

        // Case 3: 5 nights total 37,500.02 -> 37,500.02 / 5 = 7500.0040
        DailyInventoryRate d1 = new DailyInventoryRate(3L, roomCategory, checkInDate, new BigDecimal("7500.00"), 5, 0, 0L);
        DailyInventoryRate d2 = new DailyInventoryRate(4L, roomCategory, checkInDate.plusDays(1), new BigDecimal("7500.00"), 5, 0, 0L);
        DailyInventoryRate d3 = new DailyInventoryRate(5L, roomCategory, checkInDate.plusDays(2), new BigDecimal("7500.00"), 5, 0, 0L);
        DailyInventoryRate d4 = new DailyInventoryRate(6L, roomCategory, checkInDate.plusDays(3), new BigDecimal("7500.00"), 5, 0, 0L);
        DailyInventoryRate d5 = new DailyInventoryRate(7L, roomCategory, checkInDate.plusDays(4), new BigDecimal("7500.02"), 5, 0, 0L);

        when(gstTaxSlabRepository.findMatchingSlabs(eq(checkInDate), eq(new BigDecimal("7500.0040"))))
                .thenReturn(List.of(TEST_ONLY_SLAB_18));

        PriceCalculationResult result7500004 = pricingService.calculatePrice(List.of(d1, d2, d3, d4, d5), planEp, 1, null);
        assertEquals(new BigDecimal("18.00"), result7500004.getGstPercentageApplied(), "Average 7,500.004 must select 18% slab");
    }

    @Test
    void oneRoomNight_8500EP_flat1200Promo_discounted7300_gst365_total7665() {
        LocalDate d1 = LocalDate.of(2026, 11, 14);
        DailyInventoryRate r1 = new DailyInventoryRate(1L, roomCategory, d1, new BigDecimal("8500.00"), 5, 0, 0L);

        PromoCode promoFlat1200 = new PromoCode(10L, "TESTFLAT1200", DiscountType.FLAT_INR, new BigDecimal("1200.00"),
                LocalDate.of(2026, 11, 1), LocalDate.of(2027, 4, 30), 1, 100, 0, true);
        when(promoCodeRepository.findByCodeIgnoreCaseAndIsActiveTrue("TESTFLAT1200")).thenReturn(Optional.of(promoFlat1200));

        // Post-promo average tariff is 7,300.0000 -> selects 5% slab
        when(gstTaxSlabRepository.findMatchingSlabs(eq(d1), eq(new BigDecimal("7300.0000"))))
                .thenReturn(List.of(TEST_ONLY_SLAB_5));

        PriceCalculationResult result = pricingService.calculatePrice(List.of(r1), planEp, 1, "TESTFLAT1200");

        assertEquals(new BigDecimal("8500.00"), result.getSubtotalBeforeDiscountInr());
        assertEquals(new BigDecimal("1200.00"), result.getDiscountAmountInr());
        assertEquals(new BigDecimal("7300.00"), result.getTaxableAmountInr());
        assertEquals(new BigDecimal("5.00"), result.getGstPercentageApplied());
        assertEquals(new BigDecimal("365.00"), result.getGstAmountInr());
        assertEquals(new BigDecimal("7665.00"), result.getTotalPayableInr());
    }

    @Test
    void oneRoomNight_8500EP_noPromo_18Percent_gst1530_total10030() {
        LocalDate d1 = LocalDate.of(2026, 11, 14);
        DailyInventoryRate r1 = new DailyInventoryRate(1L, roomCategory, d1, new BigDecimal("8500.00"), 5, 0, 0L);

        // Pre/post-promo average tariff is 8,500.0000 -> selects 18% slab
        when(gstTaxSlabRepository.findMatchingSlabs(eq(d1), eq(new BigDecimal("8500.0000"))))
                .thenReturn(List.of(TEST_ONLY_SLAB_18));

        PriceCalculationResult result = pricingService.calculatePrice(List.of(r1), planEp, 1, null);

        assertEquals(new BigDecimal("8500.00"), result.getSubtotalBeforeDiscountInr());
        assertEquals(BigDecimal.ZERO.setScale(2), result.getDiscountAmountInr());
        assertEquals(new BigDecimal("8500.00"), result.getTaxableAmountInr());
        assertEquals(new BigDecimal("18.00"), result.getGstPercentageApplied());
        assertEquals(new BigDecimal("1530.00"), result.getGstAmountInr());
        assertEquals(new BigDecimal("10030.00"), result.getTotalPayableInr());
    }

    @Test
    void villa_14To16Nov_planCp_promoDirect10_stillGivesTotal37594_80() {
        LocalDate d1 = LocalDate.of(2026, 11, 14);
        LocalDate d2 = LocalDate.of(2026, 11, 15);

        DailyInventoryRate r1 = new DailyInventoryRate(1L, villaCategory, d1, new BigDecimal("16500.00"), 2, 0, 0L);
        DailyInventoryRate r2 = new DailyInventoryRate(2L, villaCategory, d2, new BigDecimal("16500.00"), 2, 0, 0L);

        PromoCode promo = new PromoCode(1L, "DIRECT10", DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                LocalDate.of(2026, 11, 1), LocalDate.of(2027, 4, 30), 2, 1000, 0, true);
        when(promoCodeRepository.findByCodeIgnoreCaseAndIsActiveTrue("DIRECT10")).thenReturn(Optional.of(promo));

        // Subtotal: 35,400.00, Discount: 3,540.00, Discounted: 31,860.00, Average per night: 15,930.0000 -> 18% slab
        when(gstTaxSlabRepository.findMatchingSlabs(eq(d1), eq(new BigDecimal("15930.0000"))))
                .thenReturn(List.of(TEST_ONLY_SLAB_18));

        PriceCalculationResult result = pricingService.calculatePrice(List.of(r1, r2), planCp, 1, "DIRECT10");

        assertEquals(new BigDecimal("35400.00"), result.getSubtotalBeforeDiscountInr());
        assertEquals(new BigDecimal("3540.00"), result.getDiscountAmountInr());
        assertEquals(new BigDecimal("31860.00"), result.getTaxableAmountInr());
        assertEquals(new BigDecimal("18.00"), result.getGstPercentageApplied());
        assertEquals(new BigDecimal("5734.80"), result.getGstAmountInr());
        assertEquals(new BigDecimal("37594.80"), result.getTotalPayableInr());
    }

    @Test
    void villa_14To16Nov_planEp_noPromo_unverified0PercentSlab() {
        LocalDate d1 = LocalDate.of(2026, 11, 14);
        LocalDate d2 = LocalDate.of(2026, 11, 15);

        DailyInventoryRate r1 = new DailyInventoryRate(1L, villaCategory, d1, new BigDecimal("16500.00"), 2, 0, 0L);
        DailyInventoryRate r2 = new DailyInventoryRate(2L, villaCategory, d2, new BigDecimal("16500.00"), 2, 0, 0L);

        // test values, not real GST rates
        GstTaxSlab TEST_ONLY_SLAB_0 = new GstTaxSlab(1L, BigDecimal.ZERO, null, BigDecimal.ZERO,
                LocalDate.of(2025, 9, 22), null, GstVerificationStatus.UNVERIFIED);
        when(gstTaxSlabRepository.findMatchingSlabs(eq(d1), eq(new BigDecimal("16500.0000"))))
                .thenReturn(List.of(TEST_ONLY_SLAB_0));

        PriceCalculationResult result = pricingService.calculatePrice(List.of(r1, r2), planEp, 1, null);

        assertEquals(new BigDecimal("33000.00"), result.getSubtotalBeforeDiscountInr());
        assertEquals(BigDecimal.ZERO.setScale(2), result.getDiscountAmountInr());
        assertEquals(new BigDecimal("33000.00"), result.getTaxableAmountInr());
        assertEquals(BigDecimal.ZERO.setScale(2), result.getGstAmountInr());
        assertEquals(new BigDecimal("33000.00"), result.getTotalPayableInr());
        assertEquals("UNVERIFIED", result.getGstVerificationStatus());
    }
}
