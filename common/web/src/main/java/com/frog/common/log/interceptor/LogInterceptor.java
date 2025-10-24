package com.frog.common.log.interceptor;

import com.frog.common.log.util.LogUtils;
import com.frog.common.security.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 *
 *
 * @author Deng
 * createData 2025/10/24 14:05
 * @version 1.0
 */
@Slf4j
public class LogInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        // 设置RequestId
        String requestId = UUID.randomUUID().toString().replace("-", "");
        LogUtils.setRequestId(requestId);
        response.setHeader("X-Request-Id", requestId);

        // 设置用户上下文
        Long userId = SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId().getMostSignificantBits() : null;
        String username = SecurityUtils.getCurrentUsername();
        LogUtils.setUserContext(userId, username);

        // 记录请求开始
        request.setAttribute("startTime", System.currentTimeMillis());

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        try {
            Long startTime = (Long) request.getAttribute("startTime");
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                LogUtils.api(request.getMethod(), request.getRequestURI(),
                        duration, response.getStatus());
            }
        } finally {
            LogUtils.clear();
        }
    }
}
