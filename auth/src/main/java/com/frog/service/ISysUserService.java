package com.frog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.domain.dto.UserDTO;
import com.frog.domain.dto.UserInfo;
import com.frog.domain.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
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

    String resetPassword(UUID id);

    void changePassword(UUID userId, String oldPassword, String newPassword);

    void grantRoles(UUID userId, List<UUID> roleIds);

    void lockUser(UUID id, Boolean lock);
}
