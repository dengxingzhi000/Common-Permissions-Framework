package com.frog.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存策略
 * L1: Caffeine本地缓存
 * L2: Redis分布式缓存
 *
 * @author Deng
 * createData 2025/10/24 14:10
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class MultiLevelCache {
    private final RedisTemplate<String, Object> redisTemplate;

    // L1缓存 - Caffeine
    private final Cache<@NonNull String, Object> localCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();

    /**
     * 获取缓存
     */
    public <T> T get(String key, Class<T> type) {
        // 1. 先查L1缓存
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            return type.cast(value);
        }

        // 2. 再查L2缓存
        value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            // 回填L1缓存
            localCache.put(key, value);
            return type.cast(value);
        }

        return null;
    }

    /**
     * 设置缓存
     */
    public void set(String key, Object value, Duration ttl) {
        // 同时写入L1和L2
        localCache.put(key, value);
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * 删除缓存
     */
    public void evict(String key) {
        localCache.invalidate(key);
        redisTemplate.delete(key);
    }
}