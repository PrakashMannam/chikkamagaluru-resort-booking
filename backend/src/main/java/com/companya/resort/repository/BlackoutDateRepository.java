package com.companya.resort.repository;

import com.companya.resort.entity.BlackoutDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BlackoutDateRepository extends JpaRepository<BlackoutDate, Long> {

    @Query("SELECT b FROM BlackoutDate b WHERE (b.roomCategory IS NULL OR b.roomCategory.id = :categoryId) AND b.blackoutDate >= :checkIn AND b.blackoutDate < :checkOut")
    List<BlackoutDate> findConflictingBlackouts(
            @Param("categoryId") Long categoryId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    @Query("SELECT b FROM BlackoutDate b WHERE b.blackoutDate >= :checkIn AND b.blackoutDate < :checkOut")
    List<BlackoutDate> findAllBlackoutsInRange(
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );
}
