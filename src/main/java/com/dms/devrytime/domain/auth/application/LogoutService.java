package com.dms.devrytime.domain.auth.application;

import com.dms.devrytime.domain.auth.domain.RefreshToken;
import com.dms.devrytime.domain.auth.domain.RefreshTokenRepository;
import com.dms.devrytime.domain.auth.presentation.dto.request.LogoutRequest;
import com.dms.devrytime.global.exception.DevryTimeException;
import com.dms.devrytime.global.exception.ErrorCode;
import com.dms.devrytime.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LogoutService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public void logout(LogoutRequest request, Long userId){
        jwtProvider.validateRefreshToken(request.refreshToken());

        RefreshToken savedRefreshToken = refreshTokenRepository
                .findByTokenAndUserId(request.refreshToken(), userId)
                .orElseThrow(() -> new DevryTimeException(ErrorCode.TOKEN_INVALID));

        refreshTokenRepository.delete(savedRefreshToken);
    }
}
