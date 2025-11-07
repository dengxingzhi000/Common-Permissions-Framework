package com.frog.common.dto.user;

import com.frog.common.dto.permission.PermissionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
