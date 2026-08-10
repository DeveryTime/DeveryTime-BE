package com.dms.devrytime.domain.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsBySchoolNumberAndSchoolYear(String schoolNumber, Integer schoolYear);

    Optional<User> findByEmail(String email);
}
