package com.frog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frog.domain.dto.PermissionDTO;
import com.frog.domain.dto.UserDTO;
import com.frog.domain.dto.UserInfo;
import com.frog.domain.entity.SysUser;
import com.frog.exception.BusinessException;
import com.frog.mapper.SysPermissionMapper;
import com.frog.mapper.SysUserMapper;
import com.frog.util.SecurityUtils;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Deng
 * createData 2025/10/15 11:46
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final SysUserMapper userMapper;
    private final SysPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表
     */
    public Page<UserDTO> listUsers(Integer pageNum, Integer pageSize,
                                   String username, Integer status) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(username != null && !username.isEmpty(), SysUser::getUsername, username)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreateTime);

        Page<SysUser> userPage = userMapper.selectPage(page, wrapper);

        // 转换为DTO
        Page<UserDTO> dtoPage = new Page<>(pageNum, pageSize, userPage.getTotal());
        List<UserDTO> userDTOs = userPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(userDTOs);

        return dtoPage;
    }

    /**
     * 根据ID查询用户
     */
    @Cacheable(value = "user", key = "#id")
    public UserDTO getUserById(UUID id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserDTO dto = convertToDTO(user);

        // 查询用户角色
        List<UUID> roleIds = userMapper.findRoleIdsByUserId(id);
        dto.setRoleIds(roleIds);

        // 查询角色名称
        if (!roleIds.isEmpty()) {
            List<String> roleNames = userMapper.findRoleNamesByUserId(id);
            dto.setRoleNames(roleNames);
        }

        return dto;
    }

    /**
     * 获取用户详细信息（包含权限和菜单）
     */
    @Cacheable(value = "userInfo", key = "#userId")
    public UserInfo getUserInfo(UUID userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setDeptId(user.getDeptId());
        userInfo.setUserLevel(user.getUserLevel());

        // 查询角色和权限
        Set<String> roles = userMapper.findRolesByUserId(userId);
        Set<String> permissions = userMapper.findPermissionsByUserId(userId);

        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

        // 构建菜单树（只返回菜单类型的权限）
        List<PermissionDTO> menuTree = permissionMapper.findMenuTreeByUserId(userId);
        userInfo.setMenuTree(new HashSet<>(menuTree));

        return userInfo;
    }

    /**
     * 新增用户
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, allEntries = true)
    public void addUser(UserDTO userDTO) {
        // 检查用户名是否存在
        if (userMapper.existsByUsername(userDTO.getUsername())) {
            throw new BusinessException("用户已存在");
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(userDTO, user);

        // 加密密码
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        } else {
            // 默认密码
            user.setPassword(passwordEncoder.encode("123456"));
        }

        user.setId(UuidCreator.getTimeOrderedEpoch());

        // 设置密码过期时间（90天后）
        LocalDateTime passwordExpireTime = LocalDateTime.now().plusDays(90);
        user.setPasswordExpireTime(passwordExpireTime);
        user.setForceChangePassword(1); // 首次登录强制修改密码

        user.setCreateBy(SecurityUtils.getCurrentUserId());
        user.setCreateTime(LocalDateTime.now());

        userMapper.insert(user);

        // 分配角色
        if (userDTO.getRoleIds() != null && !userDTO.getRoleIds().isEmpty()) {
            userMapper.batchInsertUserRoles(user.getId(), userDTO.getRoleIds());
        }

        log.info("User created: {}, by: {}", user.getUsername(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 修改用户
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, key = "#userDTO.id")
    public void updateUser(UserDTO userDTO) {
        SysUser existUser = userMapper.selectById(userDTO.getId());
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(userDTO, user);
        user.setUpdateBy(SecurityUtils.getCurrentUserId());
        user.setUpdateTime(LocalDateTime.now());

        // 密码不在此处修改
        user.setPassword(null);

        userMapper.updateById(user);

        // 更新角色
        if (userDTO.getRoleIds() != null) {
            userMapper.deleteUserRoles(user.getId());
            if (!userDTO.getRoleIds().isEmpty()) {
                userMapper.batchInsertUserRoles(user.getId(), userDTO.getRoleIds());
            }
        }

        log.info("User updated: {}, by: {}", user.getUsername(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 删除用户（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, key = "#id")
    public void deleteUser(UUID id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 不能删除超级管理员
        //todo
        if (user.getId().equals(1L)) {
            throw new BusinessException("不能删除超级管理员");
        }

        // 不能删除自己
        if (user.getId().equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException("不能删除当前登录用户");
        }

        userMapper.deleteById(id);

        log.info("User deleted: {}, by: {}", user.getUsername(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 重置密码
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails"}, key = "#id")
    public String resetPassword(UUID id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 生成随机密码
        String newPassword = generateRandomPassword();

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForceChangePassword(1); // 强制修改密码
        user.setUpdateBy(SecurityUtils.getCurrentUserId());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.updateById(user);

        log.info("Password reset for user: {}, by: {}",
                user.getUsername(), SecurityUtils.getCurrentUsername());

        return newPassword;
    }

    /**
     * 修改密码
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails"}, key = "#userId")
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }

        // 新密码不能与旧密码相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForceChangePassword(0);
        user.setLastPasswordChangeTime(LocalDateTime.now());

        // 更新密码过期时间
        LocalDateTime passwordExpireTime = LocalDateTime.now().plusDays(90);
        user.setPasswordExpireTime(passwordExpireTime);

        userMapper.updateById(user);

        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * 授权角色
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo", "userRoles", "userPermissions"}, key = "#userId")
    public void grantRoles(UUID userId, List<UUID> roleIds) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        // 删除原有角色
        userMapper.deleteUserRoles(userId);

        // 分配新角色
        if (roleIds != null && !roleIds.isEmpty()) {
            userMapper.batchInsertUserRoles(userId, roleIds);
        }

        log.info("Roles granted to user: {}, roles: {}, by: {}",
                user.getUsername(), roleIds, SecurityUtils.getCurrentUsername());
    }

    /**
     * 锁定/解锁用户
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails"}, key = "#id")
    public void lockUser(UUID id, Boolean lock) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (lock) {
            user.setStatus(2); // 锁定
            LocalDateTime lockedUntil = LocalDateTime.now().plusHours(24); // 锁定24小时
            user.setLockedUntil(lockedUntil);
        } else {
            user.setStatus(1); // 启用
            user.setLockedUntil(null);
            user.setLoginAttempts(0);
        }

        user.setUpdateBy(SecurityUtils.getCurrentUserId());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.updateById(user);

        log.info("User {} {}, by: {}",
                user.getUsername(), lock ? "locked" : "unlocked",
                SecurityUtils.getCurrentUsername());
    }

    // ========== 私有方法 ==========

    private UserDTO convertToDTO(SysUser user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        dto.setForceChangePassword(user.getForceChangePassword() == 1);
        dto.setTwoFactorEnabled(user.getTwoFactorEnabled() == 1);
        return dto;
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        // 确保包含大写、小写、数字和特殊字符
        password.append(chars.charAt(random.nextInt(26))); // 大写
        password.append(chars.charAt(26 + random.nextInt(26))); // 小写
        password.append(chars.charAt(52 + random.nextInt(10))); // 数字
        password.append(chars.charAt(62 + random.nextInt(4))); // 特殊字符

        // 剩余随机字符
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return shufflePassword(password.toString(), random);
    }

    private String shufflePassword(String password, SecureRandom random) {
        List<Character> charList = password.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(charList, random);

        return charList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }
}
