package com.frog.common.context;

import java.util.Set;

/**
 *
 *
 * @author Deng
 * createData 2025/10/16 16:13
 * @version 1.0
 */
public class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> CONTEXT = new ThreadLocal<>();

    public static void set(SecurityContext context) {
        CONTEXT.set(context);
    }

    public static SecurityContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static Long getUserId() {
        SecurityContext context = get();
        return context != null ? context.getUserId() : null;
    }

    public static String getUsername() {
        SecurityContext context = get();
        return context != null ? context.getUsername() : null;
    }

    public static Set<String> getRoles() {
        SecurityContext context = get();
        return context != null ? context.getRoles() : Set.of();
    }

    public static Set<String> getPermissions() {
        SecurityContext context = get();
        return context != null ? context.getPermissions() : Set.of();
    }
}
