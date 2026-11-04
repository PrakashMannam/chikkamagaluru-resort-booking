package com.companya.resort.repository;

import com.companya.resort.entity.GstTaxSlab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface GstTaxSlabRepository extends JpaRepository<GstTaxSlab, Long> {

    @Query("SELECT g FROM GstTaxSlab g WHERE g.effectiveFrom <= :date AND (g.effectiveTo IS NULL OR g.effectiveTo >= :date) AND :tariff > g.minTariffInr AND (g.maxTariffInr IS NULL OR :tariff <= g.maxTariffInr) ORDER BY g.effectiveFrom DESC")
    List<GstTaxSlab> findMatchingSlabs(@Param("date") LocalDate date, @Param("tariff") BigDecimal tariff);
}
