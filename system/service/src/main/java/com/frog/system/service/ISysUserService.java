package com.frog.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.frog.common.dto.user.UserDTO;
import com.frog.common.dto.user.UserInfo;
import com.frog.system.domain.entity.SysUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * 用户表(UUIDv7主键) 服务类
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
public interface ISysUserService extends IService<SysUser> {
    Page<UserDTO> listUsers(Integer pageNum, Integer pageSize,
                            String username, Integer status);

    UserDTO getUserById(UUID id);

    UserInfo getUserInfo(UUID userId);

    void addUser(UserDTO userDTO);

    void updateUser(UserDTO userDTO);

    void deleteUser(UUID id);

    void updateLastLogin(UUID userId, String ipAddress);
    // ==================== 密码管理 ====================

    String resetPassword(UUID id);

    void changePassword(UUID userId, String oldPassword, String newPassword);

    // ==================== 角色授予 ====================

    /**
     * 授予永久角色
     */
    void grantRoles(UUID userId, List<UUID> roleIds);

    /**
     * 授予临时角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @param effectiveTime 生效时间
     * @param expireTime 过期时间
     */
    void grantTemporaryRoles(UUID userId, List<UUID> roleIds,
                             LocalDateTime effectiveTime, LocalDateTime expireTime);

    /**
     * 延长临时角色的有效期
     */
    void extendTemporaryRole(UUID userId, UUID roleId, LocalDateTime newExpireTime);

    /**
     * 提前终止临时角色
     */
    void terminateTemporaryRole(UUID userId, UUID roleId);

    /**
     * 查询用户的临时角色列表
     */
    List<Map<String, Object>> getUserTemporaryRoles(UUID userId);

    // ==================== 账户管理 ====================

    void lockUser(UUID id, Boolean lock);

    /**
     * 检查用户是否有访问某个部门的权限
     */
    boolean canAccessDept(UUID userId, UUID deptId);

    /**
     * 获取用户的数据权限范围
     */
    Integer getUserDataScope(UUID userId);

    /**
     * 统计用户信息
     */
    Map<String, Object> getUserStatistics(UUID userId);
}
