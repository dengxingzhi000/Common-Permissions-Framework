//package com.frog.common.uaa.service;
//
//import com.frog.common.uaa.feign.SystemUserClient;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import java.util.stream.Collectors;
//
///**
// *
// *
// * @author Deng
// * createData 2025/10/24 14:36
// * @version 1.0
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class CustomUserDetailsService implements UserDetailsService {
//    private final SystemUserClient systemUserClient;
//
//    @Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        // 从system服务获取用户信息
//        var result = systemUserClient.getUserForAuth(username);
//
//        if (!result.isSuccess() || result.getData() == null) {
//            log.warn("User not found: {}", username);
//            throw new UsernameNotFoundException("用户不存在: " + username);
//        }
//
//        var userAuth = result.getData();
//
//        // 获取用户权限
//        var rolesResult = systemUserClient.getUserRoles(userAuth.getId());
//        var permissionsResult = systemUserClient.getUserPermissions(userAuth.getId());
//
//        var authorities = rolesResult.getData().stream()
//                .map(SimpleGrantedAuthority::new)
//                .collect(Collectors.toList());
//
//        authorities.addAll(permissionsResult.getData().stream()
//                .map(SimpleGrantedAuthority::new)
//                .collect(Collectors.toList()));
//
//        return User.builder()
//                .username(userAuth.getUsername())
//                .password(userAuth.getPassword())
//                .authorities(authorities)
//                .accountExpired(false)
//                .accountLocked(userAuth.getStatus() == 2)
//                .credentialsExpired(false)
//                .disabled(userAuth.getStatus() != 1)
//                .build();
//    }
//}
