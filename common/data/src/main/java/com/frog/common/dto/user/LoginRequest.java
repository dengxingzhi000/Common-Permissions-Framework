package com.frog.common.dto.user;

import lombok.Data;

/**
 *
 *
 * @author Deng
 * createData 2025/10/14 15:03
 * @version 1.0
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
    private String twoFactorCode;
    private String deviceId;
}