package com.frog.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frog.common.annotation.AuditLog;
import com.frog.common.domain.entity.SysAuditLog;
import com.frog.common.mapper.SysAuditLogMapper;
import com.frog.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计日志AOP切面
 *
 * @author Deng
 * createData 2025/10/14 17:23
 * @version 1.0
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SysAuditLogAspect {

    private final SysAuditLogMapper sysAuditLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.frog.common.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        AuditLog annotation = signature.getMethod().getAnnotation(AuditLog.class);

        // 获取请求信息
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 构建日志对象
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setUserId(SecurityUtils.getCurrentUserId());
        auditLog.setUsername(SecurityUtils.getCurrentUsername());
        auditLog.setOperationType(annotation.businessType());
        auditLog.setOperationDesc(annotation.operation());
        auditLog.setRiskLevel(annotation.riskLevel());

        if (request != null) {
            auditLog.setRequestUri(request.getRequestURI());
            auditLog.setRequestMethod(request.getMethod());
            auditLog.setIpAddress(getClientIp(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));

            // 记录请求参数
            if (annotation.recordParams()) {
                try {
                    Object[] args = point.getArgs();
                    String params = objectMapper.writeValueAsString(args);
                    // 敏感信息脱敏
                    params = desensitize(params);
                    auditLog.setRequestParams(params);
                } catch (Exception e) {
                    log.error("Failed to serialize request params", e);
                }
            }
        }

        Object result;
        try {
            // 执行方法
            result = point.proceed();
            auditLog.setStatus(1);

            // 记录响应结果
            if (annotation.recordResult() && result != null) {
                try {
                    String response = objectMapper.writeValueAsString(result);
                    auditLog.setResponseData(desensitize(response));
                } catch (Exception e) {
                    log.error("Failed to serialize response", e);
                }
            }

        } catch (Exception e) {
            auditLog.setStatus(0);
            auditLog.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            long executeTime = System.currentTimeMillis() - startTime;
            auditLog.setExecuteTime((int) executeTime);
            auditLog.setCreateTime(LocalDateTime.now());

            // 异步保存日志
            try {
                sysAuditLogMapper.insert(auditLog);
            } catch (Exception e) {
                log.error("Failed to save audit log", e);
            }
        }

        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 敏感信息脱敏
     */
    private String desensitize(String content) {
        if (content == null) return null;

        // 脱敏密码字段
        content = content.replaceAll("(\"password\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        // 脱敏身份证
        content = content.replaceAll("(\"idCard\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3");
        // 脱敏手机号
        content = content.replaceAll("(\"phone\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3");

        return content;
    }
}
