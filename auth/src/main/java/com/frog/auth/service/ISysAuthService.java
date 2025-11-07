package com.frog.auth.service;

import com.frog.common.dto.user.LoginRequest;
import com.frog.common.dto.user.LoginResponse;

import java.util.UUID;

/**
 *
 *
 * @author Deng
 * createData 2025/10/17 14:55
 * @version 1.0
 */
public interface ISysAuthService {
    LoginResponse login(LoginRequest request, String ipAddress, String deviceId);

    void logout(String token, UUID userId, String reason);

    LoginResponse refreshToken(String refreshToken, String deviceId, String ipAddress);

    void forceLogout(UUID userId, String reason);
}
