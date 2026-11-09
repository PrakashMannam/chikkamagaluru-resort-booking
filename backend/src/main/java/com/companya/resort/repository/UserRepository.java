package com.companya.resort.repository;

import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndIsActiveTrue(String email);

    boolean existsByEmail(String email);

    List<User> findBySpecialtyLaneOrderByIdAsc(SpecialtyLane specialtyLane);

    List<User> findBySpecialtyLaneAndIsActiveTrueOrderByIdAsc(SpecialtyLane specialtyLane);

    List<User> findByIsGmTrueOrderByIdAsc();

    List<User> findByIsGmTrueAndIsActiveTrueOrderByIdAsc();

    Optional<User> findFirstBySpecialtyLaneOrderByIdAsc(SpecialtyLane specialtyLane);

    Optional<User> findFirstBySpecialtyLaneAndIsActiveTrueOrderByIdAsc(SpecialtyLane specialtyLane);

    Optional<User> findFirstByIsGmTrueOrderByIdAsc();

    Optional<User> findFirstByIsGmTrueAndIsActiveTrueOrderByIdAsc();
}
