package com.dms.deverytime.domain.auth.service;

import com.dms.deverytime.domain.auth.dto.request.LoginRequest;
import com.dms.deverytime.domain.auth.dto.response.TokenResponse;
import com.dms.deverytime.domain.auth.entity.RefreshToken;
import com.dms.deverytime.domain.auth.repository.RefreshTokenRepository;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import com.dms.deverytime.global.security.jwt.JwtProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class LoginService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public TokenResponse login(LoginRequest request){

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw new DeveryTimeException(ErrorCode.INVALID_CREDENTIALS);

        String accessToken = jwtProvider.generateAccessToken(user.getId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        RefreshToken token = RefreshToken.builder()
                .token(refreshToken)
                .userId(user.getId())
                .expiresAt(jwtProvider.getExpiration(refreshToken))
                .build();

        refreshTokenRepository.save(token);

        return TokenResponse.of(accessToken, refreshToken);
    }
}
