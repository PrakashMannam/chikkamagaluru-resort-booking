package com.companya.resort.repository;

import com.companya.resort.entity.DailyInventoryRate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyInventoryRateRepository extends JpaRepository<DailyInventoryRate, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM DailyInventoryRate r WHERE r.roomCategory.id = :categoryId AND r.stayDate >= :checkIn AND r.stayDate < :checkOut ORDER BY r.stayDate ASC")
    List<DailyInventoryRate> findAndLockByRoomCategoryIdAndStayDateRange(
            @Param("categoryId") Long categoryId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    @Query("SELECT r FROM DailyInventoryRate r WHERE r.roomCategory.id = :categoryId AND r.stayDate >= :checkIn AND r.stayDate < :checkOut ORDER BY r.stayDate ASC")
    List<DailyInventoryRate> findByRoomCategoryIdAndStayDateRange(
            @Param("categoryId") Long categoryId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    Optional<DailyInventoryRate> findByRoomCategoryIdAndStayDate(Long categoryId, LocalDate stayDate);

    boolean existsByRoomCategoryIdAndStayDate(Long categoryId, LocalDate stayDate);
}
