package com.frog.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebAuthnRegisterChallengeResponse {
    private String challenge;      // base64url
    private String rpId;
    private Long timeout;          // ms
    private Map<String, Object> user; // id(name, displayName)
    private String attestation;    // none|direct|indirect (default none)
}

