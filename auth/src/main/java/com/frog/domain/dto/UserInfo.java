package com.frog.domain.dto;

import lombok.Data;

import java.util.Set;
import java.util.UUID;

/**
 *
 *
 * @author Deng
 * createData 2025/10/15 11:46
 * @version 1.0
 */
@Data
public class UserInfo {

    private UUID userId;
    private String username;
    private String realName;
    private String avatar;
    private String email;
    private String phone;
    private UUID deptId;
    private String deptName;
    private Integer userLevel;
    private Set<String> roles;
    private Set<String> permissions;
    private Set<PermissionDTO> menuTree; // 菜单树
}
