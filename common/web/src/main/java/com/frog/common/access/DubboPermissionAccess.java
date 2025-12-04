package com.frog.common.access;

import com.frog.system.api.PermissionDubboService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Dubbo-backed implementation for permission access.
 */
@Component
@Primary
@ConditionalOnClass(DubboReference.class)
public class DubboPermissionAccess implements PermissionAccessPort {
    @DubboReference
    private PermissionDubboService permissionDubboService;

    @Override
    public List<String> findPermissionsByUrl(String url, String method) {
        return permissionDubboService.findPermissionsByUrl(url, method);
    }

    @Override
    public Set<String> findAllPermissionsByUserId(UUID userId) {
        return permissionDubboService.findAllPermissionsByUserId(userId);
    }
}

