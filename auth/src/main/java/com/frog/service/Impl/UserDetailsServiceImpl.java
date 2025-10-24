package com.frog.service.Impl;

import com.frog.common.security.domain.SecurityUser;
import com.frog.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * UserDetailsService实现
 *
 * @author Deng
 * createData 2025/10/14 14:54
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {
    private final SysUserMapper sysUserMapper;

    @Override
    @Cacheable(
            value = "userDetails",
            key = "#username",
            unless = "#result == null"
    )
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户基本信息
        var user = sysUserMapper.findByUsername(username);
        if (user == null || user.getDeleted() == 1) {
            log.warn("User not found: {}", username);
            throw new UsernameNotFoundException("用户不存在或已删除: " + username);
        }

        // 查询用户角色
        Set<String> roles = sysUserMapper.findRolesByUserId(user.getId());

        // 查询用户权限（包括角色权限）
        Set<String> permissions = sysUserMapper.findPermissionsByUserId(user.getId());

        SecurityUser securityUser = SecurityUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .realName(user.getRealName())
                .deptId(user.getDeptId())
                .status(user.getStatus())
                .accountType(user.getAccountType())
                .userLevel(user.getUserLevel())
                .roles(roles)
                .permissions(permissions)
                .twoFactorEnabled(user.getTwoFactorEnabled() == 1)
                .passwordExpireTime(user.getPasswordExpireTime())
                .forceChangePassword(user.getForceChangePassword() == 1)
                .build();

        log.info("User loaded: {}, Roles: {}, Permissions count: {}",
                username, roles, permissions.size());

        return securityUser;
    }
}
