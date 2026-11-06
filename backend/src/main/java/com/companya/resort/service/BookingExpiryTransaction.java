package com.companya.resort.service;

import com.companya.resort.entity.Booking;
import com.companya.resort.entity.BookingStatus;
import com.companya.resort.entity.DailyInventoryRate;
import com.companya.resort.repository.BookingRepository;
import com.companya.resort.repository.DailyInventoryRateRepository;
import com.companya.resort.repository.PaymentRepository;
import com.companya.resort.repository.PromoCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookingExpiryTransaction {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final DailyInventoryRateRepository dailyInventoryRateRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final Clock clock;

    @Autowired
    public BookingExpiryTransaction(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            DailyInventoryRateRepository dailyInventoryRateRepository,
            PromoCodeRepository promoCodeRepository,
            Clock clock
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.dailyInventoryRateRepository = dailyInventoryRateRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.clock = clock;
    }

    public BookingExpiryTransaction(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            DailyInventoryRateRepository dailyInventoryRateRepository,
            Clock clock
    ) {
        this(bookingRepository, paymentRepository, dailyInventoryRateRepository, null, clock);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int executeExpiryBatch() {
        LocalDateTime now = LocalDateTime.now(clock);

        // 1. Lock booking rows first (SKIP LOCKED, max 100)
        List<Long> expiredBookingIds = bookingRepository.findExpiredBookingIdsForUpdateSkipLocked(now);
        if (expiredBookingIds == null || expiredBookingIds.isEmpty()) {
            return 0;
        }

        int releasedCount = 0;
        for (Long bookingId : expiredBookingIds) {
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                continue;
            }

            // Verify status and expiry
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT || booking.getExpiresAt().isAfter(now)) {
                continue;
            }

            // Mark booking expired
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            // Mark payment token used
            paymentRepository.findByBookingId(booking.getId()).ifPresent(payment -> {
                payment.setTokenUsed(true);
                payment.setTokenUsedAt(now);
                paymentRepository.save(payment);
            });

            // 2. Lock inventory rows ascending by date and release stock
            List<DailyInventoryRate> lockedRates = dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(
                    booking.getRoomCategory().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate()
            );

            for (DailyInventoryRate rate : lockedRates) {
                rate.setBookedCount(Math.max(0, rate.getBookedCount() - booking.getRoomsCount()));
                dailyInventoryRateRepository.save(rate);
            }

            // 3. Lock promo code and release usage if applicable
            if (booking.getPromoCode() != null && promoCodeRepository != null) {
                Long promoId = booking.getPromoCode().getId();
                if (promoId != null) {
                    promoCodeRepository.findAndLockById(promoId).ifPresent(promo -> {
                        int current = (promo.getCurrentUses() != null) ? promo.getCurrentUses() : 0;
                        promo.setCurrentUses(Math.max(0, current - 1));
                        promoCodeRepository.save(promo);
                    });
                }
            }

            releasedCount++;
        }

        return releasedCount;
    }
}
