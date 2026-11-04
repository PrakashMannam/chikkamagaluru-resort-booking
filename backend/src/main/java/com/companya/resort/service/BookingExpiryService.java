package com.companya.resort.service;

import com.companya.resort.entity.Booking;
import com.companya.resort.entity.BookingStatus;
import com.companya.resort.entity.DailyInventoryRate;
import com.companya.resort.repository.BookingRepository;
import com.companya.resort.repository.DailyInventoryRateRepository;
import com.companya.resort.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingExpiryService {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryService.class);

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final DailyInventoryRateRepository dailyInventoryRateRepository;
    private final Clock clock;

    public BookingExpiryService(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            DailyInventoryRateRepository dailyInventoryRateRepository,
            Clock clock
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.dailyInventoryRateRepository = dailyInventoryRateRepository;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60000)
    public void runScheduledExpiryJob() {
        try {
            int released = releaseExpiredHolds();
            if (released > 0) {
                log.info("Scheduled expiry job released {} expired booking holds", released);
            }
        } catch (Exception e) {
            log.error("Error running scheduled booking expiry job: {}", e.getMessage(), e);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now(clock);
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

            // 1. Mark booking expired (booking row was locked first by query)
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            // 2. Mark payment token used
            paymentRepository.findByBookingId(booking.getId()).ifPresent(payment -> {
                payment.setTokenUsed(true);
                payment.setTokenUsedAt(now);
                paymentRepository.save(payment);
            });

            // 3. Lock inventory rows in ascending date order and decrement booked_count
            List<DailyInventoryRate> lockedRates = dailyInventoryRateRepository.findAndLockByRoomCategoryIdAndStayDateRange(
                    booking.getRoomCategory().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate()
            );

            for (DailyInventoryRate rate : lockedRates) {
                rate.setBookedCount(Math.max(0, rate.getBookedCount() - booking.getRoomsCount()));
                dailyInventoryRateRepository.save(rate);
            }

            releasedCount++;
        }

        return releasedCount;
    }
}
