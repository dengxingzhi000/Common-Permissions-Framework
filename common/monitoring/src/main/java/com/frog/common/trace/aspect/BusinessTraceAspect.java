package com.frog.common.trace.aspect;

import com.frog.common.trace.annotation.BusinessTrace;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
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
        String operationName = businessTrace.operationName();

        // 添加自定义标签
        ActiveSpan.tag("operation", operationName);
        ActiveSpan.tag("user", SecurityUtils.getCurrentUsername());

        if (businessTrace.recordArgs()) {
            ActiveSpan.tag("args", JSON.toJSONString(point.getArgs()));
        }

        long start = System.currentTimeMillis();
        try {
            Object result = point.proceed();

            if (businessTrace.recordResult()) {
                ActiveSpan.tag("result", JSON.toJSONString(result));
            }

            return result;
        } catch (Exception e) {
            ActiveSpan.error(e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            ActiveSpan.tag("duration", String.valueOf(duration));
        }
    }
}