package com.dms.devrytime.domain.auth.application;

import com.dms.devrytime.domain.auth.domain.RefreshToken;
import com.dms.devrytime.domain.auth.domain.RefreshTokenRepository;
import com.dms.devrytime.domain.auth.presentation.dto.request.ReissueRequest;
import com.dms.devrytime.domain.auth.presentation.dto.response.TokenResponse;
import com.dms.devrytime.global.exception.DevryTimeException;
import com.dms.devrytime.global.exception.ErrorCode;
import com.dms.devrytime.global.security.jwt.JwtProvider;
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
                .orElseThrow(() -> new DevryTimeException(ErrorCode.TOKEN_INVALID));

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
