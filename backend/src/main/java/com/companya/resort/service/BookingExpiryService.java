package com.companya.resort.service;

import com.companya.resort.repository.BookingRepository;
import com.companya.resort.repository.DailyInventoryRateRepository;
import com.companya.resort.repository.PaymentRepository;
import com.companya.resort.repository.PromoCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class BookingExpiryService {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryService.class);

    private final BookingExpiryTransaction expiryTransaction;

    @Autowired
    public BookingExpiryService(BookingExpiryTransaction expiryTransaction) {
        this.expiryTransaction = expiryTransaction;
    }

    public BookingExpiryService(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            DailyInventoryRateRepository dailyInventoryRateRepository,
            PromoCodeRepository promoCodeRepository,
            Clock clock
    ) {
        this(new BookingExpiryTransaction(bookingRepository, paymentRepository, dailyInventoryRateRepository, promoCodeRepository, clock));
    }

    public BookingExpiryService(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            DailyInventoryRateRepository dailyInventoryRateRepository,
            Clock clock
    ) {
        this(new BookingExpiryTransaction(bookingRepository, paymentRepository, dailyInventoryRateRepository, null, clock));
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
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

    public int releaseExpiredHolds() {
        return expiryTransaction.executeExpiryBatch();
    }
}
