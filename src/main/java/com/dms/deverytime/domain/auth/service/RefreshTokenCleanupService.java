package com.dms.deverytime.domain.auth.service;

import com.dms.deverytime.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    public int deleteExpiredTokens() {

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        return refreshTokenRepository.deleteExpired(now);
    }
}
