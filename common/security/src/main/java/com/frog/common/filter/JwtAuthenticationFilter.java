package com.frog.common.filter;

import com.frog.common.domain.SecurityUser;
import com.frog.common.properties.JwtProperties;
import com.frog.common.util.JwtUtil;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Jwt过滤器
 *
 * @author Deng
 * createData 2025/10/11 13:49
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {

        try {
            // 获取Token
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                // 获取当前请求信息
                String currentIp = getClientIp(request);
                String currentDeviceId = getDeviceId(request);

                // 验证Token
                if (jwtUtil.validateToken(token, currentIp, currentDeviceId)) {
                    // 提取用户信息
                    UUID userId = jwtUtil.getUserIdFromToken(token);
                    String username = jwtUtil.getUsernameFromToken(token);
                    Set<String> permissions = jwtUtil.getPermissionsFromToken(token);
                    Set<String> roles = jwtUtil.getRolesFromToken(token);

                    // 构建权限列表
                    Set<SimpleGrantedAuthority> authorities = permissions.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toSet());

                    authorities.addAll(roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toSet()));

                    // 创建认证对象
                    SecurityUser userDetails = new SecurityUser();
                    userDetails.setUserId(userId);
                    userDetails.setUsername(username);
                    userDetails.setPermissions(permissions);
                    userDetails.setRoles(roles);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, authorities);

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 设置到Security上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("User authenticated: {}", username);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.getHeader());
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtProperties.getPrefix())) {
            return bearerToken.substring(jwtProperties.getPrefix().length());
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 多级代理情况下，取第一个IP
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }

    private String getDeviceId(HttpServletRequest request) {
        // 从请求头获取设备ID，客户端应该生成并保存设备ID
        String deviceId = request.getHeader("X-Device-ID");
        if (!StringUtils.hasText(deviceId)) {
            // 如果没有设备ID，使用User-Agent的hash作为临时标识
            String userAgent = request.getHeader("User-Agent");
            deviceId = userAgent != null ? String.valueOf(userAgent.hashCode()) : "unknown";
        }
        return deviceId;
    }
}
