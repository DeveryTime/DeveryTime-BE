package com.dms.devrytime.domain.auth.application;

import com.dms.devrytime.domain.auth.domain.RefreshToken;
import com.dms.devrytime.domain.auth.domain.RefreshTokenRepository;
import com.dms.devrytime.domain.auth.presentation.dto.request.LoginRequest;
import com.dms.devrytime.domain.auth.presentation.dto.response.TokenResponse;
import com.dms.devrytime.domain.user.domain.User;
import com.dms.devrytime.domain.user.domain.UserRepository;
import com.dms.devrytime.global.exception.DevryTimeException;
import com.dms.devrytime.global.exception.ErrorCode;
import com.dms.devrytime.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> new DevryTimeException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw new DevryTimeException(ErrorCode.INVALID_CREDENTIALS);

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
