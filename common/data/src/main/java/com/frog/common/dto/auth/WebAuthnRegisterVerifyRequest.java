package com.frog.common.dto.auth;

import lombok.Data;

@Data
public class WebAuthnRegisterVerifyRequest {
    private String id;                 // credential id (base64url)
    private String rawId;              // base64url
    private String type;               // public-key
    private String clientDataJSON;     // base64url
    private String attestationObject;  // base64url
    // Optional simplified path if front-end provides parsed public key
    private String publicKeyPem;       // optional
    private String alg;                // optional
}

