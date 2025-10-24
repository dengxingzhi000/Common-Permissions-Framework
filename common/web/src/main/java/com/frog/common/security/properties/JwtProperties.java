package com.frog.common.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Jwt配置类
 *
 * @author Deng
 * createData 2025/10/11 11:05
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {
    private String secret;
    private Long expiration = 3600000L; // 1小时
    private Long refreshExpiration = 604800000L; // 7天
    private String issuer = "bank-permission-system";
    private String header = "Authorization";
    private String prefix = "Bearer ";
}