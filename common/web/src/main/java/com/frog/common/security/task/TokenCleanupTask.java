package com.frog.common.security.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 定时任务 - 清理过期Token
 *
 * @author Deng
 * createData 2025/10/15 14:44
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupTask {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 每天凌晨3点清理过期的黑名单Token
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredBlacklistTokens() {
        try {
            Set<String> keys = redisTemplate.keys("jwt:blacklist:*");
            if (!keys.isEmpty()) {
                long count = 0;
                for (String key : keys) {
                    Long ttl = redisTemplate.getExpire(key);
                    if (ttl < 0) {
                        redisTemplate.delete(key);
                        count++;
                    }
                }
                log.info("Cleaned up {} expired blacklist tokens", count);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup expired tokens", e);
        }
    }

    /**
     * 每小时清理过期的会话
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupExpiredSessions() {
        try {
            Set<String> keys = redisTemplate.keys("jwt:user:*");
            if (!keys.isEmpty()) {
                long count = 0;
                for (String key : keys) {
                    Long ttl = redisTemplate.getExpire(key);
                    if (ttl < 0) {
                        redisTemplate.delete(key);
                        count++;
                    }
                }
                log.info("Cleaned up {} expired user sessions", count);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup expired sessions", e);
        }
    }
}
