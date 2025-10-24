package com.frog.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.frog.common.response.ApiResponse;
import com.frog.common.security.util.HttpServletRequestUtils;
import com.frog.common.security.util.IpUtils;
import com.frog.common.security.util.SecurityUtils;
import com.frog.common.sentinel.annotation.RateLimit;
import com.frog.domain.dto.*;
import com.frog.service.Impl.SysAuthServiceImpl;
import com.frog.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
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
    private final SysAuthServiceImpl sysAuthServiceImpl;
    private final ISysUserService userService;
    private final HttpServletRequestUtils httpServletRequestUtils;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @SentinelResource(value = "auth_login")
    @RateLimit(qps = 50)
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request,
                                            HttpServletRequest httpRequest) {
        String ipAddress = IpUtils.getClientIp(httpRequest);
        String deviceId = httpServletRequestUtils.getDeviceId(httpRequest);

        LoginResponse response = sysAuthServiceImpl.login(request, ipAddress, deviceId);

        return ApiResponse.success(response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = httpServletRequestUtils.getTokenFromRequest(request);
        UUID userId = SecurityUtils.getCurrentUserId();

        sysAuthServiceImpl.logout(token, userId, "用户主动登出");

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

        LoginResponse response = sysAuthServiceImpl.refreshToken(
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

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Validated @RequestBody ChangePasswordRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
//        authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success();
    }
}
