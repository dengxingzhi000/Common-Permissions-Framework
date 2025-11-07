package com.frog.common.mybatisPlus.domain;

import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

/**
 * 用户上下文数据类
 *
 * @author Deng
 * createData 2025/11/6 15:49
 */
@Data
@Builder
public class UserContext {
    private UUID userId;
    private String username;
    private Set<String> roles;
    private Set<String> permissions;
}
