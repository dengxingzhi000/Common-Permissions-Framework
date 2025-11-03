package com.frog.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.frog.common.response.ApiResponse;
import com.frog.common.security.util.HttpServletRequestUtils;
import com.frog.common.security.util.IpUtils;
import com.frog.common.security.util.SecurityUtils;
import com.frog.common.sentinel.annotation.RateLimit;
import com.frog.domain.dto.LoginRequest;
import com.frog.domain.dto.LoginResponse;
import com.frog.domain.dto.RefreshTokenRequest;
import com.frog.domain.dto.UserInfo;
import com.frog.service.ISysAuthService;
import com.frog.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 认证控制器
 *
 * @author Deng
 * createData 2025/10/14 17:57
 * @version 1.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class SysAuthController {
    private final ISysAuthService authService;
    private final ISysUserService userService;
    private final HttpServletRequestUtils httpServletRequestUtils;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @SentinelResource(value = "auth_login")
    @RateLimit()
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request,
                                            HttpServletRequest httpRequest) {
        String ipAddress = IpUtils.getClientIp(httpRequest);
        String deviceId = httpServletRequestUtils.getDeviceId(httpRequest);

        LoginResponse response = authService.login(request, ipAddress, deviceId);

        return ApiResponse.success(response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = httpServletRequestUtils.getTokenFromRequest(request);
        UUID userId = SecurityUtils.getCurrentUserId();

        authService.logout(token, userId, "用户主动登出");

        return ApiResponse.success();
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request,
                                              HttpServletRequest httpRequest) {
        String ipAddress = IpUtils.getClientIp(httpRequest);
        String deviceId = request.getDeviceId() != null ? request.getDeviceId() :
                httpRequest.getHeader("X-Device-ID");

        LoginResponse response = authService.refreshToken(
                request.getRefreshToken(), deviceId, ipAddress);

        return ApiResponse.success(response);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/userinfo")
    public ApiResponse<UserInfo> getUserInfo() {
        UUID userId = SecurityUtils.getCurrentUserId();

        // 从数据库查询完整用户信息
        UserInfo userInfo = userService.getUserInfo(userId);

        return ApiResponse.success(userInfo);
    }
}
