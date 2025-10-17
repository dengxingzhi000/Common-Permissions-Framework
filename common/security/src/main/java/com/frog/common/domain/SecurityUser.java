package com.frog.common.domain;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 自定义UserDetails
 *
 * @author Deng
 * createData 2025/10/14 14:52
 * @version 1.0
 */
@Data
public class SecurityUser implements UserDetails {

    private UUID userId;
    private String username;
    private String password;
    private String realName;
    private UUID deptId;
    private Integer status;
    private Integer accountType;
    private Integer userLevel;
    private Set<String> roles;
    private Set<String> permissions;

    // 安全相关字段
    private Boolean twoFactorEnabled;
    private LocalDateTime passwordExpireTime;
    private Boolean forceChangePassword;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 合并角色和权限
        Set<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        authorities.addAll(permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet()));

        return authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != 2; // 2表示锁定
    }

    @Override
    public boolean isCredentialsNonExpired() {
        if (passwordExpireTime == null) {
            return true;
        }
        return passwordExpireTime.isAfter(LocalDateTime.now());
    }

    @Override
    public boolean isEnabled() {
        return this.status == 1; // 1表示启用
    }
}
