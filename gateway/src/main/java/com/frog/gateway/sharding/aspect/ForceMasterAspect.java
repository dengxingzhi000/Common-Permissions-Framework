package com.frog.gateway.sharding.aspect;

import com.frog.gateway.sharding.annotation.ForceMaster;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 *
 *
 * @author Deng
 * createData 2025/11/11 15:59
 * @version 1.0
 */
@Aspect
@Component
@Slf4j
public class ForceMasterAspect {

    @Around("@annotation(forceMaster)")
    public Object around(ProceedingJoinPoint point, ForceMaster forceMaster) throws Throwable {
        try (HintManager hintManager = HintManager.getInstance()) {
            hintManager.setWriteRouteOnly();
            log.debug("[ForceMaster] 强制走主库: {}", point.getSignature().getName());
            return point.proceed();
        } finally {
            HintManager.clear();
        }
    }
}
