package com.frog.util;

import com.frog.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Jwt工具类
 *
 * @author Deng
 * createData 2025/10/11 11:08
 * @version 1.0
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    private static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String USER_TOKEN_PREFIX = "jwt:token:user:";
    private static final String REFRESH_TOKEN_PREFIX = "jwt:refresh:";

    public JwtTokenProvider(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getJwtSecret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public TokenResponse generateTokenPair(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date accessTokenExpiry = new Date(now.getTime() + jwtProperties.getAccessTokenValidity());
        Date refreshTokenExpiry = new Date(now.getTime() + jwtProperties.getRefreshTokenValidity());

        String jti = UUID.randomUUID().toString();

        String accessToken = Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userPrincipal.getUserId()))
                .claim("username", userPrincipal.getUsername())
                .claim("email", userPrincipal.getEmail())
                .claim("roles", userPrincipal.getRoles())
                .claim("permissions", userPrincipal.getPermissions())
                .claim("tokenType", "ACCESS")
                .issuedAt(now)
                .expiration(accessTokenExpiry)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();

        String refreshJti = UUID.randomUUID().toString();
        String refreshToken = Jwts.builder()
                .id(refreshJti)
                .subject(String.valueOf(userPrincipal.getUserId()))
                .claim("username", userPrincipal.getUsername())
                .claim("tokenType", "REFRESH")
                .issuedAt(now)
                .expiration(refreshTokenExpiry)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();

        storeTokenInRedis(userPrincipal.getUserId(), accessToken, refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenValidity / 1000)
                .build();
    }

    private void storeTokenInRedis(Long userId, String accessToken, String refreshToken) {
        String userTokenKey = USER_TOKEN_PREFIX + userId;
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + userId;

        redisTemplate.opsForValue().set(userTokenKey, accessToken,
                Duration.ofMillis(accessTokenValidity));
        redisTemplate.opsForValue().set(refreshTokenKey, refreshToken,
                Duration.ofMillis(refreshTokenValidity));
    }

    public boolean validateToken(String token) {
        try {
            String jtiFromToken = getJtiFromToken(token);

            // 检查黑名单
            if (redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + jtiFromToken)) {
                log.debug("Token is in blacklist: {}", jtiFromToken);
                return false;
            }

            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 验证token类型
            String tokenType = claims.get("tokenType", String.class);
            if (!"ACCESS".equals(tokenType)) {
                log.debug("Invalid token type: {}", tokenType);
                return false;
            }

            return true;
        } catch (ExpiredJwtException e) {
            log.error("Token expired: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("Token validation error: {}", e.getMessage());
        }
        return false;
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getJtiFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getId();
    }

    public void revokeToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String jti = claims.getId();
            Date expiration = claims.getExpiration();

            long ttl = expiration.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                        TOKEN_BLACKLIST_PREFIX + jti,
                        "revoked",
                        ttl,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            log.error("Failed to revoke token: {}", e.getMessage());
        }
    }

    public void revokeAllUserTokens(Long userId) {
        String userTokenKey = USER_TOKEN_PREFIX + userId;
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + userId;

        String accessToken = redisTemplate.opsForValue().get(userTokenKey);
        String refreshToken = redisTemplate.opsForValue().get(refreshTokenKey);

        if (accessToken != null) {
            revokeToken(accessToken);
        }
        if (refreshToken != null) {
            revokeToken(refreshToken);
        }

        redisTemplate.delete(Arrays.asList(userTokenKey, refreshTokenKey));
    }
}
