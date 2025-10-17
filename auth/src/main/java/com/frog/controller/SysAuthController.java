package com.frog.controller;

import com.frog.common.response.ApiResponse;
import com.frog.common.util.SecurityUtils;
import com.frog.domain.dto.*;
import com.frog.service.Impl.SysAuthServiceImpl;
import com.frog.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class SysAuthController {

    private final SysAuthServiceImpl sysAuthServiceImpl;
    private final ISysUserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request,
                                            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String deviceId = request.getDeviceId() != null ? request.getDeviceId() :
                httpRequest.getHeader("X-Device-ID");

        LoginResponse response = sysAuthServiceImpl.login(request, ipAddress, deviceId);
        return ApiResponse.success(response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
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
        String ipAddress = getClientIp(httpRequest);
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
        SecurityUtils.getCurrentUsername();
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

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
