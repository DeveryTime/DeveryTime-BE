package com.dms.deverytime.domain.auth.service;

import com.dms.deverytime.domain.auth.dto.request.LogoutRequest;
import com.dms.deverytime.domain.auth.entity.RefreshToken;
import com.dms.deverytime.domain.auth.repository.RefreshTokenRepository;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import com.dms.deverytime.global.security.jwt.JwtProvider;
import com.dms.deverytime.global.security.jwt.RefreshTokenHasher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class LogoutService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;

    public void logout(LogoutRequest request, Long userId){

        jwtProvider.validateRefreshToken(request.refreshToken());
        String savedRefreshTokenHash = refreshTokenHasher.hash(request.refreshToken());

        RefreshToken savedRefreshToken = refreshTokenRepository
                .findByTokenAndUserId(savedRefreshTokenHash, userId)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.TOKEN_INVALID));

        refreshTokenRepository.delete(savedRefreshToken);
    }
}
