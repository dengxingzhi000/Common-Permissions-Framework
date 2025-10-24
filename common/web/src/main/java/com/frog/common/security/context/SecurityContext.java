package com.frog.common.security.context;

import lombok.Data;

import java.util.Set;

/**
 *
 *
 * @author Deng
 * createData 2025/10/16 16:14
 * @version 1.0
 */
@Data
public class SecurityContext {
    private Long userId;
    private String username;
    private Set<String> roles;
    private Set<String> permissions;
}
