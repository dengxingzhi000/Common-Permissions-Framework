package com.frog.common.web.health;

import com.frog.common.cache.MultiLevelCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

@Component
@RequiredArgsConstructor
public class CacheHealthIndicator implements HealthIndicator {
    private final RedisTemplate<String, Object> redisTemplate;
    private final MultiLevelCache multiLevelCache;

    @Override
    public Health health() {
        try {
            String pong = redisTemplate.getRequiredConnectionFactory().getConnection().ping();
            boolean redisOk = pong != null && !pong.isEmpty();
            long l1Size = multiLevelCache.localSize();
            Health.Builder b = redisOk ? Health.up() : Health.down();
            return b.withDetail("redis", redisOk ? "UP" : "DOWN")
                    .withDetail("multilevel.l1.size", l1Size)
                    .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}

