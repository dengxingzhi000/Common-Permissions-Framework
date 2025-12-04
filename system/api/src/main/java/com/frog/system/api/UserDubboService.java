package com.frog.system.api;

import com.frog.common.dto.user.UserInfo;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dubbo API for internal high-speed RPC between services.
 */
public interface UserDubboService {

    /**
     * Fetch user info by userId.
     */
    UserInfo getUserInfo(UUID userId);

    /**
     * Update last login info.
     */
    void updateLastLogin(UUID userId, String ipAddress, LocalDateTime loginTime);
}

