package com.companya.resort.repository;

import com.companya.resort.entity.RatePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatePlanRepository extends JpaRepository<RatePlan, Long> {
    List<RatePlan> findByRoomCategoryIdAndIsActiveTrue(Long roomCategoryId);
    Optional<RatePlan> findByIdAndIsActiveTrue(Long id);
    Optional<RatePlan> findByRoomCategoryIdAndCodeIgnoreCaseAndIsActiveTrue(Long roomCategoryId, String code);
}
