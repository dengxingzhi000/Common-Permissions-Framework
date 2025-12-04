package com.frog.common.access;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Abstraction for permission access. Prefer Dubbo, fallback to Feign.
 */
public interface PermissionAccessPort {

    List<String> findPermissionsByUrl(String url, String method);

    Set<String> findAllPermissionsByUserId(UUID userId);
}

