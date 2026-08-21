package com.dms.deverytime.domain.auth.application;

import com.dms.deverytime.domain.auth.domain.RefreshToken;
import com.dms.deverytime.domain.auth.domain.RefreshTokenRepository;
import com.dms.deverytime.domain.auth.presentation.dto.request.ReissueRequest;
import com.dms.deverytime.domain.auth.presentation.dto.response.TokenResponse;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import com.dms.deverytime.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReissueService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenResponse reissue(ReissueRequest request){
        jwtProvider.validateRefreshToken(request.refreshToken());

        RefreshToken savedRefreshToken = refreshTokenRepository
                .findByToken(request.refreshToken())
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.TOKEN_INVALID));

        Long userId = savedRefreshToken.getUserId();

        refreshTokenRepository.delete(savedRefreshToken);

        String accessToken = jwtProvider.generateAccessToken(userId);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(refreshToken)
                .userId(userId)
                .expiresAt(jwtProvider.getExpiration(refreshToken))
                .build();

        refreshTokenRepository.save(newRefreshToken);

        return TokenResponse.of(accessToken, refreshToken);
    }
}
