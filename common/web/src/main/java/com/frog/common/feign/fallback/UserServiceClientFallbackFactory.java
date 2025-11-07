package com.frog.common.feign.fallback;

import com.frog.common.response.ApiResponse;
import com.frog.common.security.domain.SecurityUser;
import com.frog.common.feign.client.SysUserServiceClient;
import com.frog.common.sentinel.feign.BaseFallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * UserService降级处理
 *
 * @author Deng
 * createData 2025/10/31 9:52
 * @version 1.0
 */
@Slf4j
@Component
public class UserServiceClientFallbackFactory extends BaseFallbackFactory<SysUserServiceClient> {
    @Override
    protected SysUserServiceClient createFallback(String errorMsg, Throwable cause) {
        return new SysUserServiceClient() {
            @Override
            public ApiResponse<SecurityUser> getUserByUsername(String username) {
                log.error("调用用户服务失败: {}, 原因: {}", username, errorMsg);
                return ApiResponse.fail(503, "用户服务暂时不可用");
            }

            @Override
            public ApiResponse<Set<String>> getUserRoles(UUID userId) {
                log.error("查询用户角色失败: {}, 原因: {}", userId, errorMsg);
                return ApiResponse.success(Collections.emptySet());
            }

            @Override
            public ApiResponse<Set<String>> getUserPermissions(UUID userId) {
                log.error("查询用户权限失败: {}, 原因: {}", userId, errorMsg);
                return ApiResponse.success(Collections.emptySet());
            }

            @Override
            public ApiResponse<Void> updateLastLogin(UUID userId, String ipAddress) {
                log.error("更新登录信息失败: {}, 原因: {}", userId, errorMsg);
                return ApiResponse.fail(503, "更新登录信息失败");
            }
        };
    }
}