package com.frog.mapper;

import com.frog.domain.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("""
            SELECT * FROM sys_user
            WHERE username = #{username} AND deleted = 0
            """)
    SysUser findByUsername(@Param("username") String username);

    @Select("""
            SELECT r.role_code FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND r.status = 1 AND r.deleted = 0
            AND ur.approval_status = 1
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            """)
    Set<String> findRolesByUserId(@Param("userId") UUID userId);

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

    @Update("""
            UPDATE sys_user SET last_login_time = #{loginTime},
            last_login_ip = #{ipAddress}, login_attempts = 0
            WHERE id = #{userId}
            """)
    void updateLastLogin(@Param("userId") UUID userId,
                         @Param("ipAddress") String ipAddress,
                         @Param("loginTime") LocalDateTime loginTime);

    @Update("""
            UPDATE sys_user SET login_attempts = login_attempts + 1
            WHERE username = #{username}
            """)
    void incrementLoginAttempts(@Param("username") String username);

    @Update("""
            UPDATE sys_user SET status = 2, locked_until = #{lockedUntil}
            WHERE username = #{username}
            """)
    void lockAccount(@Param("username") String username,
                     @Param("lockedUntil") LocalDateTime lockedUntil);

    @Select("""
            SELECT COUNT(*) > 0 FROM sys_user 
            WHERE username = #{username} AND deleted = 0
            """)
    boolean existsByUsername(@Param("username") String username);

    @Select("""
            SELECT role_id FROM sys_user_role 
            WHERE user_id = #{userId}
            """)
    List<UUID> findRoleIdsByUserId(@Param("userId") UUID userId);

    @Select("""
            SELECT r.role_name FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.deleted = 0
            """)
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);

    @Delete("""
            DELETE FROM sys_user_role 
            WHERE user_id = #{userId}
            """)
    void deleteUserRoles(@Param("userId") UUID userId);

    @Insert("""
            <script>
            INSERT INTO sys_user_role (id, user_id, role_id, create_by, create_time, approval_status) VALUES
            <foreach collection='roleIds' item='roleId' separator=','>
            (UNHEX(REPLACE(UUID(), '-', '')), #{userId}, #{roleId}, #{createBy}, NOW(), 1)
            </foreach>
            </script>
            """)
    void batchInsertUserRoles(@Param("userId") UUID userId,
                              @Param("roleIds") List<UUID> roleIds,
                              @Param("createBy") UUID createBy);

    /**
     * 获取用户数据权限范围
     * 返回用户拥有的最高权限级别（数字越小权限越大）
     */
    @Select("""
            SELECT MIN(r.data_scope) FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND r.status = 1 AND r.deleted = 0
            AND ur.approval_status = 1
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            """)
    Integer getUserDataScope(@Param("userId") UUID userId);

    /**
     * 获取用户最大审批金额
     */
    @Select("""
            SELECT MAX(r.max_approval_amount) FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND r.status = 1 AND r.deleted = 0
            """)
    BigDecimal getMaxApprovalAmount(@Param("userId") UUID userId);

    /**
     * 查询用户所在部门的所有子部门ID
     */
    @Select("""
            WITH RECURSIVE dept_tree AS (
                SELECT id, parent_id FROM sys_dept 
                WHERE id = (SELECT dept_id FROM sys_user WHERE id = #{userId})
                UNION ALL
                SELECT d.id, d.parent_id FROM sys_dept d
                INNER JOIN dept_tree dt ON d.parent_id = dt.id
            )
            SELECT id FROM dept_tree
            """)
    List<UUID> findUserDeptAndChildren(@Param("userId") UUID userId);

    /**
     * 检查用户是否有指定部门的数据权限
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_user u
            INNER JOIN sys_user_role ur ON u.id = ur.user_id
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE u.id = #{userId}
            AND (
                r.data_scope = 1  -- 全部数据
                OR (r.data_scope = 3 AND u.dept_id = #{deptId})  -- 本部门
                OR (r.data_scope = 4 AND #{deptId} IN (
                    WITH RECURSIVE dept_tree AS (
                        SELECT id FROM sys_dept WHERE id = u.dept_id
                        UNION ALL
                        SELECT d.id FROM sys_dept d
                        INNER JOIN dept_tree dt ON d.parent_id = dt.id
                    )
                    SELECT id FROM dept_tree
                ))  -- 本部门及子部门
            )
            """)
    boolean hasAccessToDept(@Param("userId") UUID userId, @Param("deptId") UUID deptId);

    /**
     * 查询即将过期的用户角色（用于通知）
     */
    @Select("""
            SELECT u.id as user_id, u.username, u.email, 
                   r.role_name, ur.expire_time
            FROM sys_user u
            INNER JOIN sys_user_role ur ON u.id = ur.user_id
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.expire_time IS NOT NULL
            AND ur.expire_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL #{days} DAY)
            AND ur.approval_status = 1
            """)
    List<Map<String, Object>> findExpiringRoles(@Param("days") Integer days);

    /**
     * 查询已过期的用户角色
     */
    @Select("""
            SELECT u.id as user_id, u.username, r.role_name, ur.expire_time
            FROM sys_user u
            INNER JOIN sys_user_role ur ON u.id = ur.user_id
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.expire_time < NOW()
            AND ur.approval_status = 1
            """)
    List<Map<String, Object>> findExpiredRoles();

    /**
     * 删除已过期的用户角色
     */
    @Delete("""
            DELETE FROM sys_user_role 
            WHERE expire_time < NOW()
            AND approval_status = 1
            """)
    int deleteExpiredRoles();

    /**
     * 更新用户角色过期状态
     */
    @Update("""
            UPDATE sys_user_role 
            SET approval_status = 2
            WHERE expire_time < NOW()
            AND approval_status = 1
            """)
    int updateExpiredRolesStatus();
}
