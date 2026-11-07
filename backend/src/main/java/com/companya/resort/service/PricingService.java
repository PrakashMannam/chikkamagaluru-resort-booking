package com.companya.resort.service;

import com.companya.resort.dto.PriceCalculationResult;
import com.companya.resort.entity.*;
import com.companya.resort.exception.InvalidPromoCodeException;
import com.companya.resort.repository.GstTaxSlabRepository;
import com.companya.resort.repository.PromoCodeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PricingService {

    private final GstTaxSlabRepository gstTaxSlabRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final Clock clock;

    public PricingService(GstTaxSlabRepository gstTaxSlabRepository, PromoCodeRepository promoCodeRepository, Clock clock) {
        this.gstTaxSlabRepository = gstTaxSlabRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.clock = clock;
    }

    public PriceCalculationResult calculatePrice(
            List<DailyInventoryRate> stayRates,
            RatePlan ratePlan,
            int roomsCount,
            String promoCodeStr
    ) {
        PromoCode promo = null;
        if (promoCodeStr != null && !promoCodeStr.trim().isEmpty()) {
            promo = promoCodeRepository.findByCodeIgnoreCaseAndIsActiveTrue(promoCodeStr.trim())
                    .orElseThrow(() -> new InvalidPromoCodeException("Promo code '" + promoCodeStr + "' is invalid or inactive"));
        }
        return calculatePriceWithPromo(stayRates, ratePlan, roomsCount, promo);
    }

    public PriceCalculationResult calculatePriceWithPromo(
            List<DailyInventoryRate> stayRates,
            RatePlan ratePlan,
            int roomsCount,
            PromoCode promo
    ) {
        if (stayRates == null || stayRates.isEmpty()) {
            throw new IllegalArgumentException("Stay rates cannot be empty");
        }
        if (roomsCount < 1) {
            throw new IllegalArgumentException("Rooms count must be at least 1");
        }

        int nights = stayRates.size();
        BigDecimal totalPreTaxSubtotal = BigDecimal.ZERO;

        // Step 1: Compute nightly plan tariff and pre-tax subtotal across stay
        for (DailyInventoryRate rate : stayRates) {
            BigDecimal baseRate = rate.getBaseRateInr();
            BigDecimal planTariff;

            if (ratePlan.getPriceModifierType() == PriceModifierType.FIXED_ADDON) {
                planTariff = baseRate.add(ratePlan.getPriceModifierValue());
            } else if (ratePlan.getPriceModifierType() == PriceModifierType.PERCENTAGE) {
                BigDecimal modifierFraction = ratePlan.getPriceModifierValue()
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                planTariff = baseRate.multiply(BigDecimal.ONE.add(modifierFraction));
            } else {
                planTariff = baseRate;
            }
            planTariff = planTariff.setScale(2, RoundingMode.HALF_UP);

            BigDecimal nightlyPreTaxForRooms = planTariff.multiply(BigDecimal.valueOf(roomsCount))
                    .setScale(2, RoundingMode.HALF_UP);
            totalPreTaxSubtotal = totalPreTaxSubtotal.add(nightlyPreTaxForRooms);
        }

        totalPreTaxSubtotal = totalPreTaxSubtotal.setScale(2, RoundingMode.HALF_UP);

        // Step 2: Promo Code Application (applied to pre-tax amount)
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        PromoCode appliedPromo = null;

        if (promo != null) {
            LocalDate today = LocalDate.now(clock);

            if (today.isBefore(promo.getValidFrom()) || today.isAfter(promo.getValidTo())) {
                throw new InvalidPromoCodeException("Promo code '" + promo.getCode() + "' is not valid for the current date");
            }
            if (nights < promo.getMinNights()) {
                throw new InvalidPromoCodeException("Promo code '" + promo.getCode() + "' requires a minimum stay of " + promo.getMinNights() + " nights");
            }
            int current = (promo.getCurrentUses() != null) ? promo.getCurrentUses() : 0;
            if (promo.getMaxUses() != null && current >= promo.getMaxUses()) {
                throw new InvalidPromoCodeException("Promo code '" + promo.getCode() + "' has reached its maximum uses");
            }

            if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
                BigDecimal discountFraction = promo.getDiscountValue()
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                discountAmount = totalPreTaxSubtotal.multiply(discountFraction).setScale(2, RoundingMode.HALF_UP);
            } else if (promo.getDiscountType() == DiscountType.FLAT_INR) {
                discountAmount = promo.getDiscountValue().min(totalPreTaxSubtotal).setScale(2, RoundingMode.HALF_UP);
            }

            appliedPromo = promo;
        }

        BigDecimal discountedPreTaxAmount = totalPreTaxSubtotal.subtract(discountAmount)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // Step 3: Compute average post-promo nightly tariff per room
        BigDecimal totalRoomNights = BigDecimal.valueOf((long) nights * roomsCount);
        BigDecimal averageNightlyTariff = discountedPreTaxAmount.divide(totalRoomNights, 4, RoundingMode.HALF_UP);

        // Step 4: Exactly ONE slab lookup on the check-in date and the average post-promo nightly tariff
        LocalDate checkInDate = stayRates.get(0).getStayDate();
        List<GstTaxSlab> slabs = gstTaxSlabRepository.findMatchingSlabs(checkInDate, averageNightlyTariff);

        BigDecimal gstPercentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        GstVerificationStatus verificationStatus = GstVerificationStatus.UNVERIFIED;

        if (!slabs.isEmpty()) {
            GstTaxSlab slab = slabs.get(0);
            gstPercentage = slab.getGstPercentage();
            verificationStatus = slab.getVerificationStatus();
        }

        // Step 5: GST Calculation: GST = DiscountedPreTax x slab rate
        BigDecimal totalGst = discountedPreTaxAmount.multiply(
                gstPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
        ).setScale(2, RoundingMode.HALF_UP);

        BigDecimal grandTotal = discountedPreTaxAmount.add(totalGst).setScale(2, RoundingMode.HALF_UP);

        return new PriceCalculationResult(
                totalPreTaxSubtotal,
                discountAmount,
                discountedPreTaxAmount,
                gstPercentage,
                totalGst,
                grandTotal,
                verificationStatus.name(),
                appliedPromo
        );
    }
}
