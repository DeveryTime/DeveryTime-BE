package com.dms.devrytime.global.security.jwt;

import com.dms.devrytime.global.exception.DevryTimeException;
import com.dms.devrytime.global.exception.ErrorCode;
import com.dms.devrytime.global.security.auth.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";

    public JwtProvider(JwtProperties jwtProperties, CustomUserDetailsService customUserDetailsService){
        this.customUserDetailsService = customUserDetailsService;
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId){
        return generateToken(userId, ACCESS_TOKEN, jwtProperties.accessTokenExpiration());
    }

    public String generateRefreshToken(Long userId){
        return generateToken(userId, REFRESH_TOKEN, jwtProperties.refreshTokenExpiration());
    }

    private String generateToken(Long userId, String type, Long time){
        Date now = new Date();

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + time))
                .claim("type", type)
                .signWith(secretKey)
                .compact();
    }

    public Claims getClaims(String token){
        try{
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e){
            throw new DevryTimeException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new DevryTimeException(ErrorCode.TOKEN_INVALID);
        }
    }

    public Long getUserId(String token){
        return Long.valueOf(getClaims(token).getSubject());
    }

    public Authentication getAuthentication(String token){
        Long userId = getUserId(token);
        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername(userId.toString());

        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
    }
}
