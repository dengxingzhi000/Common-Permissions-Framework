package com.frog.common.feign.client;

import com.frog.common.response.ApiResponse;
import com.frog.common.feign.fallback.PermissionServiceClientFallbackFactory;
import com.frog.system.domain.dto.PermissionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;
import java.util.UUID;
/**
 * 权限服务Feign客户端
 * 用于服务间调用
 *
 * @author Deng
 * createData 2025/11/6 15:29
 * @version 1.0
 */
@FeignClient(
        name = "permission-service",
        path = "/api/system/permissions",
        fallbackFactory = PermissionServiceClientFallbackFactory.class
)
public interface SysPermissionServiceClient {

    /**
     * 查询权限树
     */
    @GetMapping("/tree")
    ApiResponse<List<PermissionDTO>> getPermissionTree();

    /**
     * 查询用户权限
     */
    @GetMapping("/internal/user/{userId}")
    ApiResponse<Set<String>> getUserPermissions(@PathVariable("userId") UUID userId);

    /**
     * 查询用户是否有指定权限
     */
    @GetMapping("/internal/user/{userId}/has-permission")
    ApiResponse<Boolean> hasPermission(@PathVariable("userId") UUID userId,
                                       @RequestParam("permissionCode") String permissionCode);

    /**
     * 根据ID获取权限详情
     */
    @GetMapping("/internal/{id}")
    ApiResponse<PermissionDTO> getPermissionById(@PathVariable("id") UUID id);
}