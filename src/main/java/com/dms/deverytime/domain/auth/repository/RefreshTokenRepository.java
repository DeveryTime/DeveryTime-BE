package com.dms.deverytime.domain.auth.repository;

import com.dms.deverytime.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByTokenAndUserId(String token, Long userId);

    @Modifying
    @Query("""
        delete from RefreshToken r
        where r.expiresAt < :now
    """)
    int deleteExpired(@Param("now") LocalDateTime now);
}
