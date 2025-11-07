package com.frog.common.mybatisPlus.util;

import com.frog.common.mybatisPlus.domain.UserContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 *
 *
 * @author Deng
 * createData 2025/11/6 15:48
 */
@Component
public class UserContextUtil {
    private static final ThreadLocal<UserContext> contextHolder = new ThreadLocal<>();

    public static void setContext(UserContext context) {
        contextHolder.set(context);
    }

    public static UserContext getContext() {
        return contextHolder.get();
    }

    public static UUID getCurrentUserId() {
        UserContext context = getContext();
        return context != null ? context.getUserId() : null;
    }

    public static void clear() {
        contextHolder.remove();
    }
}

