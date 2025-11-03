package com.frog.common.sentinel.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

/**
 * 基于Redis的限流器  支持多种限流算法
 *
 * @author Deng
 * createData 2025/10/31 10:23
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiter {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * 固定窗口限流
     *
     * @param key 限流key
     * @param limit 限流数量
     * @param window 时间窗口（秒）
     * @return 是否允许通过
     */
    public boolean tryAcquireFixedWindow(String key, int limit, int window) {
        String fullKey = RATE_LIMIT_PREFIX + "fixed:" + key;

        Long count = redisTemplate.opsForValue().increment(fullKey);

        if (count == null) {
            return false;
        }

        if (count == 1) {
            redisTemplate.expire(fullKey, Duration.ofSeconds(window));
        }

        boolean allowed = count <= limit;

        if (!allowed) {
            log.warn("Rate limit exceeded: key={}, count={}, limit={}", key, count, limit);
        }

        return allowed;
    }

    /**
     * 滑动窗口限流（基于Sorted Set）
     * 更精确，但性能稍差
     *
     * @param key 限流key
     * @param limit 限流数量
     * @param window 时间窗口（秒）
     * @return 是否允许通过
     */
    public boolean tryAcquireSlidingWindow(String key, int limit, int window) {
        String fullKey = RATE_LIMIT_PREFIX + "sliding:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - window * 1000L;

        // Lua脚本：原子性操作
        String script =
                "redis.call('zremrangebyscore', KEYS[1], 0, ARGV[1]) " +
                        "local count = redis.call('zcard', KEYS[1]) " +
                        "if count < tonumber(ARGV[2]) then " +
                        "    redis.call('zadd', KEYS[1], ARGV[3], ARGV[4]) " +
                        "    redis.call('expire', KEYS[1], ARGV[5]) " +
                        "    return 1 " +
                        "else " +
                        "    return 0 " +
                        "end";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);

        Long result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(fullKey),
                String.valueOf(windowStart),
                String.valueOf(limit),
                String.valueOf(now),
                String.valueOf(now),
                String.valueOf(window)
        );

        boolean allowed = Long.valueOf(1).equals(result);

        if (!allowed) {
            log.warn("Sliding window rate limit exceeded: key={}, limit={}", key, limit);
        }

        return allowed;
    }

    /**
     * 令牌桶限流
     *
     * @param key 限流key
     * @param capacity 桶容量
     * @param rate 令牌生成速率（每秒）
     * @return 是否允许通过
     */
    public boolean tryAcquireTokenBucket(String key, int capacity, int rate) {
        String fullKey = RATE_LIMIT_PREFIX + "token:" + key;

        // Lua脚本：令牌桶算法
        String script =
                "local capacity = tonumber(ARGV[1]) " +
                        "local rate = tonumber(ARGV[2]) " +
                        "local now = tonumber(ARGV[3]) " +
                        "local requested = 1 " +

                        "local token_key = KEYS[1] .. ':tokens' " +
                        "local timestamp_key = KEYS[1] .. ':timestamp' " +

                        "local last_tokens = tonumber(redis.call('get', token_key)) " +
                        "if last_tokens == nil then " +
                        "    last_tokens = capacity " +
                        "end " +

                        "local last_time = tonumber(redis.call('get', timestamp_key)) " +
                        "if last_time == nil then " +
                        "    last_time = 0 " +
                        "end " +

                        "local delta = math.max(0, now - last_time) " +
                        "local filled_tokens = math.min(capacity, last_tokens + (delta * rate / 1000)) " +
                        "local allowed = filled_tokens >= requested " +
                        "local new_tokens = filled_tokens " +

                        "if allowed then " +
                        "    new_tokens = filled_tokens - requested " +
                        "end " +

                        "redis.call('setex', token_key, 86400, new_tokens) " +
                        "redis.call('setex', timestamp_key, 86400, now) " +

                        "return allowed and 1 or 0";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);

        Long result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(fullKey),
                String.valueOf(capacity),
                String.valueOf(rate),
                String.valueOf(System.currentTimeMillis())
        );

        boolean allowed = Long.valueOf(1).equals(result);

        if (!allowed) {
            log.warn("Token bucket rate limit exceeded: key={}", key);
        }

        return allowed;
    }

    /**
     * 漏桶限流
     *
     * @param key 限流key
     * @param capacity 桶容量
     * @param rate 漏出速率（每秒）
     * @return 是否允许通过
     */
    public boolean tryAcquireLeakyBucket(String key, int capacity, int rate) {
        String fullKey = RATE_LIMIT_PREFIX + "leaky:" + key;

        // Lua脚本：漏桶算法
        String script =
                "local capacity = tonumber(ARGV[1]) " +
                        "local rate = tonumber(ARGV[2]) " +
                        "local now = tonumber(ARGV[3]) " +
                        "local requested = 1 " +

                        "local water_key = KEYS[1] .. ':water' " +
                        "local timestamp_key = KEYS[1] .. ':timestamp' " +

                        "local last_water = tonumber(redis.call('get', water_key)) " +
                        "if last_water == nil then " +
                        "    last_water = 0 " +
                        "end " +

                        "local last_time = tonumber(redis.call('get', timestamp_key)) " +
                        "if last_time == nil then " +
                        "    last_time = now " +
                        "end " +

                        "local delta = math.max(0, now - last_time) " +
                        "local leaked_water = delta * rate / 1000 " +
                        "local current_water = math.max(0, last_water - leaked_water) " +
                        "local allowed = (current_water + requested) <= capacity " +

                        "if allowed then " +
                        "    current_water = current_water + requested " +
                        "    redis.call('setex', water_key, 86400, current_water) " +
                        "    redis.call('setex', timestamp_key, 86400, now) " +
                        "    return 1 " +
                        "else " +
                        "    return 0 " +
                        "end";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);

        Long result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(fullKey),
                String.valueOf(capacity),
                String.valueOf(rate),
                String.valueOf(System.currentTimeMillis())
        );

        boolean allowed = Long.valueOf(1).equals(result);

        if (!allowed) {
            log.warn("Leaky bucket rate limit exceeded: key={}", key);
        }

        return allowed;
    }

    /**
     * 获取剩余配额
     */
    public long getRemainingQuota(String key) {
        String fullKey = RATE_LIMIT_PREFIX + "fixed:" + key;
        Object value = redisTemplate.opsForValue().get(fullKey);
        return value != null ? Long.parseLong(value.toString()) : 0;
    }

    /**
     * 重置限流计数
     */
    public void reset(String key) {
        String pattern = RATE_LIMIT_PREFIX + "*:" + key;
        redisTemplate.keys(pattern).forEach(redisTemplate::delete);
    }
}
