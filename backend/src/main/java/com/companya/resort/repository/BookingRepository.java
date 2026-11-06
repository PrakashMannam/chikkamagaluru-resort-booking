package com.companya.resort.repository;

import com.companya.resort.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.roomCategory LEFT JOIN FETCH b.ratePlan LEFT JOIN FETCH b.promoCode WHERE b.bookingRef = :bookingRef")
    Optional<Booking> findByBookingRef(@Param("bookingRef") String bookingRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.roomCategory LEFT JOIN FETCH b.ratePlan LEFT JOIN FETCH b.promoCode WHERE b.bookingRef = :bookingRef")
    Optional<Booking> findAndLockByBookingRef(@Param("bookingRef") String bookingRef);

    @Query(value = "SELECT id FROM bookings WHERE status = 'PENDING_PAYMENT' AND expires_at <= :now ORDER BY id ASC LIMIT 100 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Long> findExpiredBookingIdsForUpdateSkipLocked(@Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.roomCategory LEFT JOIN FETCH b.ratePlan LEFT JOIN FETCH b.promoCode WHERE b.idempotencyKey = :idempotencyKey")
    Optional<Booking> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = com.companya.resort.entity.BookingStatus.PENDING_PAYMENT AND b.expiresAt > :now AND (b.guestEmail = :email OR b.guestPhone = :phone)")
    long countActiveHoldsByGuest(@Param("email") String email, @Param("phone") String phone, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b JOIN FETCH b.roomCategory JOIN FETCH b.ratePlan LEFT JOIN FETCH b.promoCode WHERE b.checkInDate = :date ORDER BY b.id ASC")
    List<Booking> findByCheckInDateWithDetails(@Param("date") java.time.LocalDate date);
}
