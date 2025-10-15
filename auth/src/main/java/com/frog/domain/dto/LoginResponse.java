package com.frog.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

/**
 *
 *
 * @author Deng
 * createData 2025/10/14 15:03
 * @version 1.0
 */
@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UUID userId;
    private String username;
    private String realName;
    private Set<String> roles;
    private Set<String> permissions;
    private Boolean needChangePassword;
    private String message;
}
