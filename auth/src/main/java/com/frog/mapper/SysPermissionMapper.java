package com.frog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frog.domain.dto.PermissionDTO;
import com.frog.domain.entity.SysPermission;
import org.apache.ibatis.annotations.*;

import java.io.Serializable;
import java.security.Permission;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * 权限表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
    /**
     * 查询用户权限
     */
    @Select("""
            SELECT DISTINCT p.permission_code FROM sys_permission p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND p.status = 1 AND p.deleted = 0
            """)
    Set<String> findPermissionsByUserId(@Param("userId") UUID userId);

    /**
     * 查询用户角色
     */
    @Select("""
            SELECT DISTINCT r.role_code FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0
            """)
    Set<String> findRolesByUserId(@Param("userId") UUID userId);

    /**
     * 查询角色权限树
     */
    @Select("""
            SELECT * FROM sys_permission WHERE status = 1 AND deleted = 0
            ORDER BY sort_order ASC
            """)
    List<SysPermission> findPermissionTree();

    /**
     * 根据角色ID查询权限
     */
    @Select("""
            SELECT p.* FROM sys_permission p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            WHERE rp.role_id = #{roleId} AND p.status = 1 AND p.deleted = 0
            """)
    List<Permission> findPermissionsByRoleId(@Param("roleId") UUID roleId);

    /**
     * 检查资源权限
     */
    @Select("""
            <script>
            SELECT COUNT(*) > 0 FROM sys_user_role ur
            INNER JOIN sys_role_permission rp ON ur.role_id = rp.role_id
            INNER JOIN sys_permission p ON rp.permission_id = p.id
            WHERE ur.user_id = #{userId}
            AND p.permission_code = #{permission}
            AND p.status = 1
            </script>
            """)
    boolean checkResourcePermission(@Param("userId") UUID userId,
                                    @Param("resourceType") String resourceType,
                                    @Param("resourceId") Serializable resourceId,
                                    @Param("permission") String permission);

    /**
     * 查询用户菜单树
     */
    @Select("""
            SELECT DISTINCT p.* FROM sys_permission p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND p.permission_type IN (1, 2)
            AND p.visible = 1 AND p.status = 1 AND p.deleted = 0
            ORDER BY p.sort_order ASC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "children", column = "id",
                    many = @Many(select = "findChildrenPermissions"))
    })
    List<PermissionDTO> findMenuTreeByUserId(@Param("userId") UUID userId);

    /**
     * 查询子权限
     */
    @Select("""
            SELECT * FROM sys_permission
            WHERE parent_id = #{parentId}
            AND permission_type IN (1, 2)
            AND visible = 1 AND status = 1 AND deleted = 0
            ORDER BY sort_order ASC
            """)
    List<PermissionDTO> findChildrenPermissions(@Param("parentId") UUID parentId);

    /**
     * 根据URL和方法查询需要的权限
     */
    @Select("""
            SELECT permission_code FROM sys_permission
            WHERE api_path = #{url}
            AND (http_method = #{method} OR http_method = '*')
            AND status = 1 AND deleted = 0
            """)
    List<String> findPermissionsByUrl(@Param("url") String url,
                                      @Param("method") String method);

    /**
     * 检查权限编码是否存在
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_permission
            WHERE permission_code = #{permissionCode} AND deleted = 0
            """)
    boolean existsByPermissionCode(@Param("permissionCode") String permissionCode);

    /**
     * 统计使用该权限的角色数
     */
    @Select("""
            SELECT COUNT(*) FROM sys_role_permission WHERE permission_id = #{permissionId}
            """)
    Integer countRolesByPermissionId(@Param("permissionId") UUID permissionId);
}
