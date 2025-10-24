package com.frog.service;

import com.frog.domain.dto.LoginRequest;
import com.frog.domain.dto.LoginResponse;

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
}
