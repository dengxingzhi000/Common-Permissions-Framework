package com.frog.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 用户详细服务
 *
 * @author Deng
 * createData 2025/10/11 13:56
 * @version 1.0
 */
@Service
@Transactional
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionService permissionService;

    public CustomUserDetailsService(UserRepository userRepository, PermissionService permissionService) {
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username));

        return createUserDetails(user);
    }

    public CustomUserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with id: " + userId));

        return createUserDetails(user);
    }

    private CustomUserDetails createUserDetails(User user) {
        Set<String> roles = permissionService.getUserRoles(user.getId());
        Set<String> permissions = permissionService.getUserPermissions(user.getId());

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));

        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));

        return CustomUserDetails.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .enabled(user.getStatus() == 1)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .accountNonLocked(user.getStatus() != 2) // 2表示锁定
                .authorities(authorities)
                .roles(roles)
                .permissions(permissions)
                .build();
    }
}
