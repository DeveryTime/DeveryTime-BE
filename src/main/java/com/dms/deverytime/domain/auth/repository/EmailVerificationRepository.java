package com.dms.deverytime.domain.auth.repository;

import com.dms.deverytime.domain.auth.entity.EmailVerification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmailVerification e where e.email = :email")
    Optional<EmailVerification> findByEmailWithLock(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmailVerification e where e.id = :id")
    Optional<EmailVerification> findByIdWithLock(@Param("id") Long id);

    @Modifying
    @Query("""
        update EmailVerification e
        set e.verificationAttemptCount = e.verificationAttemptCount + 1
        where e.id = :id
            and e.verificationAttemptCount < :maxAttempts
    """)
    int increaseAttemptCount(@Param("id") Long id, @Param("maxAttempts") int maxAttempts);
}
