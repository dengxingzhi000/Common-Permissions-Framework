package com.frog.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenUpgradeResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
}

