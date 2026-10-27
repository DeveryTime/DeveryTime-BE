package com.dms.deverytime.domain.auth.service;

import com.dms.deverytime.domain.auth.dto.request.ReissueRequest;
import com.dms.deverytime.domain.auth.dto.response.TokenResponse;
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
public class ReissueService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;

    public TokenResponse reissue(ReissueRequest request){
        jwtProvider.validateRefreshToken(request.refreshToken());

        String savedRefreshTokenHash = refreshTokenHasher.hash(request.refreshToken());

        RefreshToken savedRefreshToken = refreshTokenRepository
                .findByToken(savedRefreshTokenHash)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.TOKEN_INVALID));

        Long userId = savedRefreshToken.getUserId();

        refreshTokenRepository.delete(savedRefreshToken);

        String accessToken = jwtProvider.generateAccessToken(userId);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        String refreshTokenHash = refreshTokenHasher.hash(refreshToken);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(refreshTokenHash)
                .userId(userId)
                .expiresAt(jwtProvider.getExpiration(refreshToken))
                .build();

        refreshTokenRepository.save(newRefreshToken);

        return TokenResponse.of(accessToken, refreshToken);
    }
}
