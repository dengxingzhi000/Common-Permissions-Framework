package com.frog.common.trace.aspect;

import com.alibaba.fastjson2.JSON;
import com.frog.common.trace.annotation.BusinessTrace;
import com.frog.common.web.util.SecurityUtils;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 *
 *
 * @author Deng
 * createData 2025/11/10 9:39
 * @version 1.0
 */
@Aspect
@Component
public class BusinessTraceAspect {

    @Around("@annotation(businessTrace)")
    public Object around(ProceedingJoinPoint point, BusinessTrace businessTrace) throws Throwable {
        // 1) 操作名兜底：注解优先，否则用 Class.method
        String operationName = businessTrace.operationName();
        if (operationName == null || operationName.isBlank()) {
            MethodSignature sig = (MethodSignature) point.getSignature();
            operationName = sig.getDeclaringType().getSimpleName() + "." + sig.getMethod().getName();
        }
        ActiveSpan.tag("operation", operationName);

        // 2) 用户标签：容错 + 限长，避免 NPE/超长
        String username = null;
        try {
            username = SecurityUtils.getCurrentUsername().orElse(null);
        } catch (Throwable ignore) { /* 保底不影响主流程 */ }
        if (username != null && !username.isBlank()) {
            ActiveSpan.tag("user", truncate(username, 128));
        }

        // 可选：若有 ActiveSpan.isNoop() 可用，提前快速返回，减少无追踪时的开销
        // if (ActiveSpan.isNoop()) return point.proceed();

        // 3) 入参：仅在配置需要时序列化，限制长度并兜底异常
        if (businessTrace.recordArgs()) {
            Object[] args = point.getArgs();
            ActiveSpan.tag("argsCount", String.valueOf(args == null ? 0 : args.length));
            ActiveSpan.tag("args", toJsonLimited(args, 2048));
        }

        long startNs = System.nanoTime();
        try {
            Object result = point.proceed();

            // 4) 返回：仅在配置需要时序列化，限制长度
            if (businessTrace.recordResult()) {
                ActiveSpan.tag("result", toJsonLimited(result, 4096));
            }
            return result;
        } catch (Throwable e) {
            // 5) 错误标签精简且安全（限长），并保留原有 ActiveSpan.error
            ActiveSpan.tag("error.type", e.getClass().getName());
            String msg = e.getMessage();
            if (msg != null && !msg.isBlank()) {
                ActiveSpan.tag("error.message", truncate(msg, 512));
            }
            ActiveSpan.error(e);
            throw e;
        } finally {
            // 6) 纳秒计时，记录为毫秒
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
            ActiveSpan.tag("duration.ms", String.valueOf(elapsedMs));
        }
    }

    // 安全序列化：容错 + 限长，避免巨型对象或 toString/JSON 异常拖垮
    private static String toJsonLimited(Object obj, int maxLen) {
        if (obj == null) return "null";
        try {
            String s = JSON.toJSONString(obj);
            return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
        } catch (Throwable t) {
            return "<json-error:" + t.getClass().getSimpleName() + ">";
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

}