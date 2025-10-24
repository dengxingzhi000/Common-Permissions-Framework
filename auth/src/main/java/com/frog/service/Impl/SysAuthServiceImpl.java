package com.frog.service.Impl;

import com.frog.common.metrics.BusinessMetrics;
import com.frog.common.security.domain.SecurityUser;
import com.frog.common.security.properties.JwtProperties;
import com.frog.common.security.properties.SecurityProperties;
import com.frog.common.security.util.JwtUtils;
import com.frog.common.log.service.ISysAuditLogService;
import com.frog.domain.dto.LoginRequest;
import com.frog.domain.dto.LoginResponse;
import com.frog.mapper.SysUserMapper;
import com.frog.service.ISysAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 认证服务
 *
 * @author Deng
 * createData 2025/10/14 15:00
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysAuthServiceImpl implements ISysAuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final SysUserMapper sysUserMapper;
    private final ISysAuditLogService auditLogService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityProperties securityProperties;
    private final JwtProperties jwtProperties;
    private final BusinessMetrics businessMetrics;

    private static final String LOGIN_ATTEMPTS_PREFIX = "login:attempts:";
    private static final String ACCOUNT_LOCK_PREFIX = "account:lock:";

    /**
     * 用户登录
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String deviceId) {
        String username = request.getUsername();

        // 1. 检查账户是否被锁定
        if (isAccountLocked(username)) {
            auditLogService.recordLoginFailure(username, ipAddress, "账户已锁定");
            throw new LockedException("账户已锁定，请稍后再试");
        }

        // 2. 检查登录失败次数
        int attempts = getLoginAttempts(username);
        if (attempts >= securityProperties.getMaxLoginAttempts()) {
            lockAccount(username);
            auditLogService.recordLoginFailure(username, ipAddress, "登录失败次数过多");
            throw new LockedException("登录失败次数过多，账户已被锁定");
        }

        try {
            // 3. 执行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );

            SecurityUser user = (SecurityUser) authentication.getPrincipal();

            // 4. 检查双因素认证
            if (user.getTwoFactorEnabled() && !verifyTwoFactor(request.getTwoFactorCode(), user.getUserId())) {
                auditLogService.recordLoginFailure(username, ipAddress, "双因素认证失败");
                throw new BadCredentialsException("双因素认证失败");
            }

            // 5. 检查密码是否过期
            if (user.getPasswordExpireTime() != null && user.getPasswordExpireTime().isBefore(LocalDateTime.now())) {
                auditLogService.recordLogin(user.getUserId(), username, ipAddress, true, "密码已过期");
                return LoginResponse.builder()
                        .needChangePassword(true)
                        .message("密码已过期，请修改密码")
                        .build();
            }

            // 6. 生成Token
            Set<String> roles = user.getRoles();
            Set<String> permissions = user.getPermissions();

            String accessToken = jwtUtils.generateAccessToken(
                    user.getUserId(), username, roles, permissions, deviceId, ipAddress);
            String refreshToken = jwtUtils.generateRefreshToken(
                    user.getUserId(), username, deviceId);

            // 7. 清除登录失败记录
            clearLoginAttempts(username);

            // 8. 更新最后登录信息
            sysUserMapper.updateLastLogin(user.getUserId(), ipAddress, LocalDateTime.now());

            // 9. 记录登录日志
            auditLogService.recordLogin(user.getUserId(), username, ipAddress, true, "登录成功");

            // 10. 记录登录成功指标
            businessMetrics.recordLogin(true, deviceId);

            log.info("User login success: {}, IP: {}, Device: {}", username, ipAddress, deviceId);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtProperties.getExpiration() / 1000)
                    .userId(user.getUserId())
                    .username(username)
                    .realName(user.getRealName())
                    .roles(roles)
                    .permissions(permissions)
                    .needChangePassword(user.getForceChangePassword())
                    .build();

        } catch (AuthenticationException e) {
            // 认证失败，增加失败次数
            incrementLoginAttempts(username);
            auditLogService.recordLoginFailure(username, ipAddress, e.getMessage());

            int remainingAttempts = securityProperties.getMaxLoginAttempts() - getLoginAttempts(username);
            String message = "用户名或密码错误";
            if (remainingAttempts > 0) {
                message += "，还可尝试 " + remainingAttempts + " 次";
            }

            log.warn("Login failed for user: {}, IP: {}, Reason: {}", username, ipAddress, e.getMessage());

            businessMetrics.recordLogin(false, deviceId);
            throw new BadCredentialsException(message);
        }
    }

    /**
     * 用户登出
     */
    public void logout(String token, UUID userId, String reason) {
        jwtUtils.revokeToken(token, reason != null ? reason : "用户主动登出");
        auditLogService.recordLogout(userId, "登出成功");
        log.info("User logout: UserId={}", userId);
    }

    /**
     * 刷新Token
     */
    public LoginResponse refreshToken(String refreshToken, String deviceId, String ipAddress) {
        if (!jwtUtils.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("刷新令牌无效或已过期");
        }

        UUID userId = jwtUtils.getUserIdFromToken(refreshToken);
        String username = jwtUtils.getUsernameFromToken(refreshToken);

        // 重新获取用户权限
        Set<String> roles = sysUserMapper.findRolesByUserId(userId);
        Set<String> permissions = sysUserMapper.findPermissionsByUserId(userId);

        String newAccessToken = jwtUtils.refreshToken(
                refreshToken, roles, permissions, deviceId, ipAddress);

        log.info("Token refreshed for user: {}", username);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .userId(userId)
                .username(username)
                .build();
    }

    /**
     * 强制用户下线
     */
    public void forceLogout(UUID userId, String reason) {
        jwtUtils.revokeAllUserTokens(userId);
        auditLogService.recordLogout(userId, "管理员强制下线: " + reason);
        log.info("User force logout: UserId={}, Reason={}", userId, reason);
    }

    private boolean isAccountLocked(String username) {
        String lockKey = ACCOUNT_LOCK_PREFIX + username;
        return redisTemplate.hasKey(lockKey);
    }

    private void lockAccount(String username) {
        String lockKey = ACCOUNT_LOCK_PREFIX + username;
        redisTemplate.opsForValue().set(lockKey, System.currentTimeMillis(),
                Duration.ofMillis(securityProperties.getLockDuration()));
        log.warn("Account locked: {}", username);
    }

    private int getLoginAttempts(String username) {
        String attemptKey = LOGIN_ATTEMPTS_PREFIX + username;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptKey);
        return attempts != null ? attempts : 0;
    }

    private void incrementLoginAttempts(String username) {
        String attemptKey = LOGIN_ATTEMPTS_PREFIX + username;
        redisTemplate.opsForValue().increment(attemptKey);
        redisTemplate.expire(attemptKey, Duration.ofMillis(securityProperties.getLockDuration()));
    }

    private void clearLoginAttempts(String username) {
        String attemptKey = LOGIN_ATTEMPTS_PREFIX + username;
        redisTemplate.delete(attemptKey);
    }

    private boolean verifyTwoFactor(String code, UUID userId) {
        // TODO: 实现TOTP双因素认证验证逻辑
        // 可以使用Google Authenticator的库
        return StringUtils.hasText(code);
    }
}