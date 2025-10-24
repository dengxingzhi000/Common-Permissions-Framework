package com.frog.common.security.util;

import com.frog.common.security.properties.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Jwt工具类
 *
 * @author Deng
 * createData 2025/10/11 11:08
 * @version 1.0
 */
@Component
@Slf4j
public class JwtUtils {
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    // 添加一个默认密钥用于开发环境
    @Value("${jwt.secret:t+gG4GvjtxpXiYSW64mTNVK2TmnwtvHNXrp0TGjrGz9sd5XzzFJ7bw83puCeMoVS8Yp+9pRl78FK0L8XI3zlcg==}")
    private String defaultSecret;

    JwtUtils(JwtProperties jwtProperties, RedisTemplate<String, Object> redisTemplate) {
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
    }

    private static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String USER_TOKEN_PREFIX = "jwt:user:";
    private static final String TOKEN_FINGERPRINT_PREFIX = "jwt:fingerprint:";

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        String secret = jwtProperties.getSecret();
        
        // 如果配置文件中的密钥为空，则使用默认密钥
        if (!StringUtils.hasText(secret)) {
            log.warn("JWT secret not found in configuration, using default secret");
            secret = defaultSecret;
        }
        
        Assert.hasText(secret, "JWT secret must be configured or have a default value");
        
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException("JWT secret must be at least 512 bits for HS512, current length: " + keyBytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT signing key initialized successfully");
    }

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(UUID userId, String username, Set<String> roles,
                                      Set<String> permissions, String deviceId, String ipAddress) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("tokenType", "access");
        claims.put("deviceId", deviceId);
        claims.put("ipAddress", ipAddress);

        String jti = UUID.randomUUID().toString();
        claims.put("jti", jti);

        String token = createToken(claims, String.valueOf(userId), jwtProperties.getExpiration());

        // 存储Token到Redis，用于会话管理
        String redisKey = USER_TOKEN_PREFIX + userId + ":" + deviceId;
        redisTemplate.opsForValue().set(redisKey, token, Duration.ofMillis(jwtProperties.getExpiration()));

        // 存储设备指纹
        String fingerprintKey = TOKEN_FINGERPRINT_PREFIX + jti;
        Map<String, Object> fingerprint = new HashMap<>();
        fingerprint.put("userId", userId);
        fingerprint.put("deviceId", deviceId);
        fingerprint.put("ipAddress", ipAddress);
        fingerprint.put("issueTime", System.currentTimeMillis());
        redisTemplate.opsForHash().putAll(fingerprintKey, fingerprint);
        redisTemplate.expire(fingerprintKey, Duration.ofMillis(jwtProperties.getExpiration()));

        return token;
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(UUID userId, String username, String deviceId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("tokenType", "refresh");
        claims.put("deviceId", deviceId);
        claims.put("jti", UUID.randomUUID().toString());

        return createToken(claims, String.valueOf(userId), jwtProperties.getRefreshExpiration());
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtProperties.getIssuer())
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * 获取Claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to parse token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从Token中提取用户ID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object uuid = claims.get("userId");
        return uuid instanceof UUID ? (UUID) uuid : null;
    }

    /**
     * 从Token中提取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (String) claims.get("username") : null;
    }

    /**
     * 从Token中提取角色
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) return Collections.emptySet();
        List<String> roleList = (List<String>) claims.get("roles");
        return roleList != null ? new HashSet<>(roleList) : Collections.emptySet();
    }

    /**
     * 从Token中提取权限
     */
    @SuppressWarnings("unchecked")
    public Set<String> getPermissionsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) return Collections.emptySet();
        List<String> permList = (List<String>) claims.get("permissions");
        return permList != null ? new HashSet<>(permList) : Collections.emptySet();
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String token, String currentIp, String currentDeviceId) {
        try {
            // 1. 检查Token是否在黑名单
            if (isTokenBlacklisted(token)) {
                log.warn("Token is blacklisted");
                return false;
            }

            // 2. 解析Token
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 3. 验证Token类型
            String tokenType = (String) claims.get("tokenType");
            if (!"access".equals(tokenType)) {
                log.warn("Invalid token type: {}", tokenType);
                return false;
            }

            // 4. 验证过期时间
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                log.warn("Token has expired");
                return false;
            }

            // 5. 验证设备ID（防止Token被盗用）
            String tokenDeviceId = (String) claims.get("deviceId");
            if (tokenDeviceId != null && !tokenDeviceId.equals(currentDeviceId)) {
                log.warn("Device ID mismatch. Token device: {}, Current device: {}",
                        tokenDeviceId, currentDeviceId);
                return false;
            }

            // 6. 验证IP地址（可选，根据安全级别决定）
            String tokenIp = (String) claims.get("ipAddress");
            if (tokenIp != null && !tokenIp.equals(currentIp)) {
                log.warn("IP address changed. Token IP: {}, Current IP: {}", tokenIp, currentIp);
                // 可以根据业务需求决定是否允许IP变更
                return false;
            }

            // 7. 验证Token指纹
            String jti = (String) claims.get("jti");
            if (!validateTokenFingerprint(jti, getUserIdFromToken(token))) {
                log.warn("Token fingerprint validation failed");
                return false;
            }

            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired");
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported");
        } catch (MalformedJwtException e) {
            log.error("JWT token is malformed");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty");
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 验证Token指纹
     */
    private boolean validateTokenFingerprint(String jti, UUID userId) {
        String fingerprintKey = TOKEN_FINGERPRINT_PREFIX + jti;
        Map<Object, Object> fingerprint = redisTemplate.opsForHash().entries(fingerprintKey);

        if (fingerprint.isEmpty()) {
            return false;
        }

        UUID storedUserId = switch (fingerprint.get("userId")) {
            case UUID uuid -> uuid;
            case String uuid -> UUID.fromString(uuid);
            case null -> throw new IllegalArgumentException("userId is null");
            default -> throw new IllegalArgumentException("userId is not a UUID or String");
        };

        return userId.equals(storedUserId);
    }

    /**
     * 注销Token - 加入黑名单
     */
    public void revokeToken(String token, String reason) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return;
            }

            String jti = (String) claims.get("jti");
            Date expiration = claims.getExpiration();
            UUID userId = getUserIdFromToken(token);
            String deviceId = (String) claims.get("deviceId");

            // 计算剩余有效期
            long ttl = expiration.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                // 加入黑名单
                String blacklistKey = TOKEN_BLACKLIST_PREFIX + jti;
                Map<String, Object> blacklistInfo = new HashMap<>();
                blacklistInfo.put("revokeTime", System.currentTimeMillis());
                blacklistInfo.put("reason", reason);
                blacklistInfo.put("userId", userId);

                redisTemplate.opsForHash().putAll(blacklistKey, blacklistInfo);
                redisTemplate.expire(blacklistKey, Duration.ofMillis(ttl));

                // 删除用户Token缓存
                String userTokenKey = USER_TOKEN_PREFIX + userId + ":" + deviceId;
                redisTemplate.delete(userTokenKey);

                // 删除Token指纹
                String fingerprintKey = TOKEN_FINGERPRINT_PREFIX + jti;
                redisTemplate.delete(fingerprintKey);

                log.info("Token revoked. UserId: {}, Reason: {}", userId, reason);
            }
        } catch (Exception e) {
            log.error("Failed to revoke token: {}", e.getMessage());
        }
    }

    /**
     * 检查Token是否在黑名单
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return true;
            }
            String jti = (String) claims.get("jti");
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + jti;
            return redisTemplate.hasKey(blacklistKey);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 刷新Token
     */
    public String refreshToken(String refreshToken, Set<String> roles, Set<String> permissions,
                               String deviceId, String ipAddress) {
        if (!validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        UUID userId = getUserIdFromToken(refreshToken);
        String username = getUsernameFromToken(refreshToken);

        return generateAccessToken(userId, username, roles, permissions, deviceId, ipAddress);
    }

    /**
     * 验证刷新Token是否合法（true=合法，false=非法）
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return false;
            }
            String tokenType = (String) claims.get("tokenType");
            Date expiration = claims.getExpiration();
            return "refresh".equals(tokenType) && expiration.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 强制用户所有设备下线
     */
    public void revokeAllUserTokens(UUID userId) {
        Set<String> keys = redisTemplate.keys(USER_TOKEN_PREFIX + userId + ":*");
        if (!keys.isEmpty()) {
            for (String key : keys) {
                String token = (String) redisTemplate.opsForValue().get(key);
                if (token != null) {
                    revokeToken(token, "Admin forced logout");
                }
            }
        }
        log.info("All tokens revoked for user: {}", userId);
    }
}