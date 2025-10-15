package com.frog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.frog.domain.dto.PermissionDTO;
import com.frog.domain.entity.SysPermission;
import com.frog.exception.BusinessException;
import com.frog.mapper.SysPermissionMapper;
import com.frog.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限服务
 *
 * @author Deng
 * createData 2025/10/14 15:01
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysPermissionMapper sysPermissionMapper;

    /**
     * 检查用户是否有指定权限
     */
    @Cacheable(value = "userPermissions", key = "#userId + ':' + #permissionCode")
    public boolean hasPermission(UUID userId, String permissionCode) {
        Set<String> permissions = sysPermissionMapper.findPermissionsByUserId(userId);
        return permissions.contains(permissionCode);
    }

    /**
     * 检查用户是否有资源权限
     */
    public boolean hasResourcePermission(UUID userId, String resourceType,
                                         Serializable resourceId, String permission) {
        // 实现基于资源的权限控制
        // 例如：检查用户是否可以访问特定部门的数据
        return sysPermissionMapper.checkResourcePermission(userId, resourceType, resourceId, permission);
    }

    /**
     * 获取用户角色
     */
    @Cacheable(value = "userRoles", key = "#userId")
    public Set<String> getUserRoles(UUID userId) {
        return sysPermissionMapper.findRolesByUserId(userId);
    }

    /**
     * 获取用户权限
     */
    @Cacheable(value = "userPermissions", key = "#userId")
    public Set<String> getUserPermissions(UUID userId) {
        return sysPermissionMapper.findPermissionsByUserId(userId);
    }

    /**
     * 获取权限树
     */
    @Cacheable(value = "permissionTree", key = "'all'")
    public List<PermissionDTO> getPermissionTree() {
        List<SysPermission> permissions = sysPermissionMapper.findPermissionTree();

        // 转换为DTO
        List<PermissionDTO> permissionDTOs = permissions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 构建树形结构
        return buildTree(permissionDTOs);
    }

    /**
     * 根据ID查询权限
     */
    @Cacheable(value = "permission", key = "#id")
    public PermissionDTO getPermissionById(UUID id) {
        SysPermission permission = sysPermissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException("权限不存在");
        }
        return convertToDTO(permission);
    }

    /**
     * 新增权限
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"permission", "permissionTree", "userPermissions", "userInfo"}, allEntries = true)
    public void addPermission(PermissionDTO permissionDTO) {
        // 检查权限编码是否存在
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getPermissionCode, permissionDTO.getPermissionCode());
        if (sysPermissionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("权限编码已存在");
        }

        SysPermission permission = new SysPermission();
        copyPropertiesFromDTO(permissionDTO, permission);
        permission.setCreateBy(SecurityUtils.getCurrentUserId());
        permission.setCreateTime(LocalDateTime.now());

        sysPermissionMapper.insert(permission);
    }

    /**
     * 修改权限
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"permission", "permissionTree", "userPermissions", "userInfo"}, allEntries = true)
    public void updatePermission(PermissionDTO permissionDTO) {
        SysPermission existPermission = sysPermissionMapper.selectById(permissionDTO.getId());
        if (existPermission == null) {
            throw new BusinessException("权限不存在");
        }

        // 不能修改父节点为自己或自己的子节点
        if (permissionDTO.getParentId() != null && permissionDTO.getParentId().equals(permissionDTO.getId())) {
            throw new BusinessException("父节点不能是自己");
        }

        SysPermission permission = new SysPermission();
        copyPropertiesFromDTO(permissionDTO, permission);
        permission.setUpdateBy(SecurityUtils.getCurrentUserId());
        permission.setUpdateTime(LocalDateTime.now());

        sysPermissionMapper.updateById(permission);
    }

    /**
     * 删除权限
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"permission", "permissionTree", "userPermissions", "userInfo"}, allEntries = true)
    public void deletePermission(UUID id) {
        SysPermission permission = sysPermissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException("权限不存在");
        }

        // 检查是否有子权限
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getParentId, id);
        Long childCount = sysPermissionMapper.selectCount(wrapper);
        if (childCount > 0) {
            throw new BusinessException("该权限下还有子权限，不能删除");
        }

//         检查是否有角色使用该权限
//         注意：这里需要根据实际的表结构和关联关系来实现
         Integer roleCount = sysPermissionMapper.countRolesByPermissionId(id);
         if (roleCount > 0) {
             throw new BusinessException("该权限已被" + roleCount + "个角色使用，不能删除");
         }

        sysPermissionMapper.deleteById(id);
    }

    // ========== 私有方法 ==========

    private void copyPropertiesFromDTO(PermissionDTO permissionDTO, SysPermission permission) {
        BeanUtils.copyProperties(permissionDTO, permission);
        permission.setNeedApproval(permissionDTO.getNeedApproval() ? 1 : 0);
        permission.setNeedTwoFactor(permissionDTO.getNeedTwoFactor() ? 1 : 0);
        permission.setVisible(permissionDTO.getVisible() ? 1 : 0);
    }

    private PermissionDTO convertToDTO(SysPermission permission) {
        PermissionDTO dto = new PermissionDTO();
        BeanUtils.copyProperties(permission, dto);
        dto.setNeedApproval(permission.getNeedApproval() != null && permission.getNeedApproval() == 1);
        dto.setNeedTwoFactor(permission.getNeedTwoFactor() != null && permission.getNeedTwoFactor() == 1);
        dto.setVisible(permission.getVisible() != null && permission.getVisible() == 1);
        return dto;
    }

    /**
     * 构建树形结构
     */
    private List<PermissionDTO> buildTree(List<PermissionDTO> permissions) {
        // 创建一个Map来存储所有节点，key为id，value为PermissionDTO
        Map<UUID, PermissionDTO> permissionMap = new HashMap<>();
        for (PermissionDTO permission : permissions) {
            permissionMap.put(permission.getId(), permission);
        }

        // 构建树形结构
        List<PermissionDTO> tree = new ArrayList<>();
        for (PermissionDTO permission : permissions) {
            if (permission.getParentId() == null) {
                // 根节点
                buildTreeChildren(permission, permissionMap);
                tree.add(permission);
            }
        }

        return tree;
    }

    /**
     * 递归构建子节点
     */
    private void buildTreeChildren(PermissionDTO parent, Map<UUID, PermissionDTO> permissionMap) {
        List<PermissionDTO> children = new ArrayList<>();
        for (PermissionDTO permission : permissionMap.values()) {
            if (permission.getParentId() != null && permission.getParentId().equals(parent.getId())) {
                buildTreeChildren(permission, permissionMap); // 递归构建子节点的子节点
                children.add(permission);
            }
        }
        parent.setChildren(children);
    }
}