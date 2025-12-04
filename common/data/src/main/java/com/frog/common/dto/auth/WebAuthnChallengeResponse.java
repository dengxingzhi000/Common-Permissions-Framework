package com.frog.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebAuthnChallengeResponse {
    private String challenge;          // base64url
    private String rpId;
    private Long timeout;              // ms
    private List<Map<String, Object>> allowCredentials; // id(type), transports
    private String userVerification;   // preferred/required
}

