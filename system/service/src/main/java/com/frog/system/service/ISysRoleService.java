package com.frog.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.frog.common.dto.role.RoleDTO;
import com.frog.system.domain.entity.SysRole;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 角色表 服务类
 * </p>
 *
 * @author author
 * @since 2025-10-15
 */
public interface ISysRoleService extends IService<SysRole> {
    Page<RoleDTO> listRoles(Integer pageNum, Integer pageSize, String roleName);

    List<RoleDTO> listAllRoles();

    RoleDTO getRoleById(UUID id);

    void addRole(RoleDTO roleDTO);

    void updateRole(RoleDTO roleDTO);

    void deleteRole(UUID id);

    void grantPermissions(UUID roleId, List<UUID> permissionIds);

    List<UUID> getRolePermissionIds(UUID roleId);
}
