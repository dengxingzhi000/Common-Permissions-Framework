package com.frog.auth.service.Impl;

import com.frog.auth.service.ICustomAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

/**
 * 实现自定义授权服务
 *
 * @author Deng
 * createData 2025/11/10 10:30
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class CustomAuthorizationServiceImpl implements ICustomAuthorizationService {
    private final OAuth2AuthorizationService authorizationService;

    @Override
    public OAuth2Authorization createAuthorization(RegisteredClient client, Authentication principal,
                                                   Set<String> authorizedScopes) {

        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(client)
                .principalName(principal.getName())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(authorizedScopes);

        // 生成授权码
        OAuth2AuthorizationCode authorizationCode = new OAuth2AuthorizationCode(
                generateAuthorizationCode(),
                Instant.now(),
                Instant.now().plusSeconds(300) // 5分钟过期
        );

        builder.token(authorizationCode);

        OAuth2Authorization authorization = builder.build();
        authorizationService.save(authorization);

        return authorization;
    }

    private String generateAuthorizationCode() {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(SecureRandom.getInstanceStrong().generateSeed(32));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}