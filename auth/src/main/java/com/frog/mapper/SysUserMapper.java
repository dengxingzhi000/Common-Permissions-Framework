package com.frog.mapper;

import com.frog.domain.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * 用户表(UUIDv7主键) Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    /**
     * 根据用户名查询用户
     */
    @Select("""
            SELECT * FROM sys_user
            WHERE username = #{username} AND deleted = 0
            """)
    SysUser findByUsername(@Param("username") String username);

    /**
     * 查询用户的所有角色
     */
    @Select("""
            SELECT r.role_code FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND r.status = 1 AND r.deleted = 0
            AND ur.approval_status = 1
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            """)
    Set<String> findRolesByUserId(@Param("userId") UUID userId);

    /**
     * 查询用户的所有权限（包括角色权限）
     */
    @Select("""
            SELECT DISTINCT p.permission_code FROM sys_permission p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND p.status = 1 AND p.deleted = 0
            AND ur.approval_status = 1
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            """)
    Set<String> findPermissionsByUserId(@Param("userId") UUID userId);

    /**
     * 更新最后登录信息
     */
    @Update("""
            UPDATE sys_user SET last_login_time = #{loginTime},
            last_login_ip = #{ipAddress}, login_attempts = 0
            WHERE id = #{userId}
            """)
    void updateLastLogin(@Param("userId") UUID userId,
                         @Param("ipAddress") String ipAddress,
                         @Param("loginTime") LocalDateTime loginTime);

    /**
     * 更新登录失败次数
     */
    @Update("""
            UPDATE sys_user SET login_attempts = login_attempts + 1
            WHERE username = #{username}
            """)
    void incrementLoginAttempts(@Param("username") String username);

    /**
     * 锁定账户
     */
    @Update("""
            UPDATE sys_user SET status = 2, locked_until = #{lockedUntil}
            WHERE username = #{username}
            """)
    void lockAccount(@Param("username") String username, @Param("lockedUntil") Date lockedUntil);

    /**
     * 检查用户名是否存在
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_user 
            WHERE username = #{username} AND deleted = 0
            """)
    boolean existsByUsername(@Param("username") String username);

    /**
     * 查询用户角色ID列表
     */
    @Select("""
            SELECT role_id FROM sys_user_role 
            WHERE user_id = #{userId}
            """)
    List<UUID> findRoleIdsByUserId(@Param("userId") UUID userId);

    /**
     * 查询用户角色名称列表
     */
    @Select("""
            SELECT r.role_name FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.deleted = 0
            """)
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);

    /**
     * 删除用户角色关联
     */
    @Delete("""
            DELETE FROM sys_user_role 
            WHERE user_id = #{userId}
            """)
    void deleteUserRoles(@Param("userId") UUID userId);

    /**
     * 批量插入用户角色
     */
    @Insert("""
            <script>
            INSERT INTO sys_user_role (user_id, role_id, create_by, create_time) VALUES
            <foreach collection='roleIds' item='roleId' separator=','>
            (#{userId}, #{roleId}, #{createBy}, NOW())
            </foreach>
            </script>
            """)
    void batchInsertUserRoles(@Param("userId") UUID userId,
                              @Param("roleIds") List<UUID> roleIds,
                              @Param("createBy") UUID createBy);

    /**
     * 获取用户数据权限范围
     */
    @Select("""
            SELECT r.data_scope FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            ORDER BY r.data_scope ASC LIMIT 1
            """)
    Integer getUserDataScope(@Param("userId") UUID userId);

    /**
     * 获取用户最大审批金额
     */
    @Select("""
            SELECT MAX(r.max_approval_amount) FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            """)
    BigDecimal getMaxApprovalAmount(@Param("userId") UUID userId);
}
