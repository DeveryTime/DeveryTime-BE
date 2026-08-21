package com.dms.deverytime.domain.auth.application;

import com.dms.deverytime.domain.auth.domain.RefreshToken;
import com.dms.deverytime.domain.auth.domain.RefreshTokenRepository;
import com.dms.deverytime.domain.auth.presentation.dto.request.LogoutRequest;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import com.dms.deverytime.global.security.jwt.JwtProvider;
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
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.TOKEN_INVALID));

        refreshTokenRepository.delete(savedRefreshToken);
    }
}
