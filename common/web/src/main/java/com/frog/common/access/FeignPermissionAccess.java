package com.frog.common.access;

import com.frog.common.feign.client.SysPermissionServiceClient;
import com.frog.common.response.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Feign-backed fallback implementation for permission access.
 */
@Component
@ConditionalOnMissingBean(PermissionAccessPort.class)
public class FeignPermissionAccess implements PermissionAccessPort {
    private final SysPermissionServiceClient permissionServiceClient;

    public FeignPermissionAccess(SysPermissionServiceClient permissionServiceClient) {
        this.permissionServiceClient = permissionServiceClient;
    }

    @Override
    public List<String> findPermissionsByUrl(String url, String method) {
        return permissionServiceClient.findPermissionsByUrl(url, method);
    }

    @Override
    public Set<String> findAllPermissionsByUserId(UUID userId) {
        ApiResponse<Set<String>> resp = permissionServiceClient.getUserPermissions(userId);
        return resp != null ? resp.data() : Set.of();
    }
}

