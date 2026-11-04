package com.companya.resort.repository;

import com.companya.resort.entity.RoomCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomCategoryRepository extends JpaRepository<RoomCategory, Long> {
    Optional<RoomCategory> findByCode(String code);
}
