package com.frog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.frog.domain.dto.PermissionDTO;
import com.frog.domain.entity.SysPermission;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * 权限表 服务类
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
public interface ISysPermissionService extends IService<SysPermission> {
    boolean hasPermission(UUID userId, String permissionCode);

    boolean hasResourcePermission(UUID userId, String resourceType,
                                  Serializable resourceId, String permission);

    Set<String> getUserRoles(UUID userId);

    Set<String> getUserPermissions(UUID userId);

    List<PermissionDTO> getPermissionTree();

    PermissionDTO getPermissionById(UUID id);

    void addPermission(PermissionDTO permissionDTO);

    void updatePermission(PermissionDTO permissionDTO);

    void deletePermission(UUID id);
}
