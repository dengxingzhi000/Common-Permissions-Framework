package com.frog.common.feign.fallback;


import com.frog.common.dto.permission.PermissionDTO;
import com.frog.common.feign.client.SysPermissionServiceClient;
import com.frog.common.feign.factory.BaseFallbackFactory;
import com.frog.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 权限服务Feign客户端
 * 用于服务间调用
 *
 * @author Deng
 * createData 2025/11/6 15:30
 */
@Slf4j
@Component
public class PermissionServiceClientFallbackFactory extends BaseFallbackFactory<SysPermissionServiceClient> {

    @Override
    protected SysPermissionServiceClient createFallback(String errorMsg, Throwable cause) {
        return new SysPermissionServiceClient() {
            @Override
            public ApiResponse<List<PermissionDTO>> getPermissionTree() {
                log.error("调用权限服务查询权限树失败: {}", errorMsg, cause);
                return ApiResponse.success(new ArrayList<>());
            }

            @Override
            public ApiResponse<Set<String>> getUserPermissions(UUID userId) {
                log.error("调用权限服务查询用户权限失败: userId={}, 原因: {}", userId, errorMsg, cause);
                return ApiResponse.success(Collections.emptySet());
            }

            @Override
            public ApiResponse<Boolean> hasPermission(UUID userId, String permissionCode) {
                log.error("调用权限服务检查用户权限失败: userId={}, permissionCode={}, 原因: {}", userId, permissionCode, errorMsg, cause);
                return ApiResponse.success(Boolean.FALSE);
            }

            @Override
            public ApiResponse<PermissionDTO> getPermissionById(UUID id) {
                log.error("调用权限服务查询权限详情失败: id={}, 原因: {}", id, errorMsg, cause);
                return ApiResponse.fail(503, "权限服务暂时不可用");
            }

            @Override
            public List<String> findPermissionsByUrl(String url, String method) {
                log.error("调用权限服务查询权限失败: url={}, method={}, 错误信息: {}", url, method, errorMsg, cause);
                return Collections.emptyList();
            }

            @Override
            public Set<String> findAllPermissionsByUserId(UUID userId) {
                log.error("调用权限服务查询用户权限失败: userId={}, 错误信息: {}", userId, errorMsg, cause);
                return Collections.emptySet();
            }

            @Override
            public List<Map<String, Object>> findApiPermissions() {
                log.error("调用权限服务查询接口权限失败: 错误信息: {}", errorMsg, cause);
                return Collections.emptyList();
            }
        };
    }
}
