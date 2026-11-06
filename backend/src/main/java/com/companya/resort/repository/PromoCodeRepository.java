package com.companya.resort.repository;

import com.companya.resort.entity.PromoCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeIgnoreCaseAndIsActiveTrue(String code);

    Optional<PromoCode> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PromoCode p WHERE LOWER(p.code) = LOWER(:code) AND p.isActive = true")
    Optional<PromoCode> findAndLockByCodeIgnoreCaseAndIsActiveTrue(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PromoCode p WHERE p.id = :id")
    Optional<PromoCode> findAndLockById(@Param("id") Long id);
}
