package com.companya.resort.repository;

import com.companya.resort.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingId(Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.booking.id = :bookingId")
    Optional<Payment> findAndLockByBookingId(@Param("bookingId") Long bookingId);

    Optional<Payment> findByOneTimePaymentToken(String oneTimePaymentToken);
}
