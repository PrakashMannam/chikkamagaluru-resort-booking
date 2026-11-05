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

    boolean existsByEmail(String email);

    List<User> findBySpecialtyLaneOrderByIdAsc(SpecialtyLane specialtyLane);

    List<User> findByIsGmTrueOrderByIdAsc();

    Optional<User> findFirstBySpecialtyLaneOrderByIdAsc(SpecialtyLane specialtyLane);

    Optional<User> findFirstByIsGmTrueOrderByIdAsc();
}
