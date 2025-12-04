package com.frog.common.redis.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类
 * 基于Redis实现
 *
 * @author Deng
 * createData 2025/10/31 10:08
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLock {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final Long RELEASE_SUCCESS = 1L;

    // Lua脚本：原子性释放锁
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('del', KEYS[1]) " +
                    "else " +
                    "    return 0 " +
                    "end";

    /**
     * 尝试获取锁
     *
     * @param lockKey 锁的key
     * @param expireTime 过期时间
     * @return 锁的唯一标识，获取失败返回null
     */
    public String tryLock(String lockKey, Duration expireTime) {
        String lockId = UUID.randomUUID().toString();
        String fullKey = LOCK_PREFIX + lockKey;

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(fullKey, lockId, expireTime);

        return Boolean.TRUE.equals(success) ? lockId : null;
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的key
     * @param lockId 锁的唯一标识
     * @return 是否释放成功
     */
    public boolean unlock(String lockKey, String lockId) {
        String fullKey = LOCK_PREFIX + lockKey;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(UNLOCK_SCRIPT);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(
                script,
                Collections.singletonList(fullKey),
                lockId
        );

        return RELEASE_SUCCESS.equals(result);
    }

    /**
     * 自旋获取锁
     *
     * @param lockKey 锁的key
     * @param expireTime 过期时间
     * @param waitTime 最大等待时间
     * @param retryInterval 重试间隔
     * @return 锁的唯一标识，超时返回null
     */
    public String tryLockWithRetry(String lockKey, Duration expireTime,
                                   Duration waitTime, Duration retryInterval) {
        long deadline = System.currentTimeMillis() + waitTime.toMillis();

        while (System.currentTimeMillis() < deadline) {
            String lockId = tryLock(lockKey, expireTime);
            if (lockId != null) {
                return lockId;
            }

            try {
                TimeUnit.MILLISECONDS.sleep(retryInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Lock retry interrupted", e);
                return null;
            }
        }

        log.warn("Failed to acquire lock after waiting: {}", waitTime);
        return null;
    }

    /**
     * 执行带锁的操作（自动获取和释放锁）
     *
     * @param lockKey 锁的key
     * @param expireTime 锁的过期时间
     * @param action 需要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws IllegalStateException 获取锁失败
     */
    public <T> T executeWithLock(String lockKey, Duration expireTime,
                                 Supplier<T> action) {
        String lockId = tryLock(lockKey, expireTime);

        if (lockId == null) {
            throw new IllegalStateException("Failed to acquire lock: " + lockKey);
        }

        try {
            return action.get();
        } finally {
            releaseLock(lockKey, lockId);
        }
    }

    /**
     * 执行带锁的操作（带重试）
     *
     * @param lockKey 锁的key
     * @param expireTime 锁的过期时间
     * @param waitTime 最大等待时间
     * @param retryInterval 重试间隔
     * @param action 需要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws IllegalStateException 获取锁失败
     */
    public <T> T executeWithLockRetry(String lockKey, Duration expireTime,
                                      Duration waitTime, Duration retryInterval,
                                      Supplier<T> action) {
        String lockId = tryLockWithRetry(lockKey, expireTime, waitTime, retryInterval);

        if (lockId == null) {
            throw new IllegalStateException("Failed to acquire lock: " + lockKey);
        }

        try {
            return action.get();
        } finally {
            releaseLock(lockKey, lockId);
        }
    }

    /**
     * 执行带锁的操作（无返回值）
     */
    public void executeWithLock(String lockKey, Duration expireTime, Runnable action) {
        String lockId = tryLock(lockKey, expireTime);

        if (lockId == null) {
            throw new IllegalStateException("Failed to acquire lock: " + lockKey);
        }

        try {
            action.run();
        } finally {
            releaseLock(lockKey, lockId);
        }
    }

    /**
     * 检查锁是否存在
     */
    public boolean isLocked(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        return redisTemplate.hasKey(fullKey);
    }

    /**
     * 续期锁（延长过期时间）
     *
     * @param lockKey 锁的key
     * @param lockId 锁的唯一标识
     * @param expireTime 新的过期时间
     * @return 是否续期成功
     */
    public boolean renewLock(String lockKey, String lockId, Duration expireTime) {
        String fullKey = LOCK_PREFIX + lockKey;

        // Lua脚本：只有持有锁的才能续期
        String renewScript =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "    return redis.call('expire', KEYS[1], ARGV[2]) " +
                        "else " +
                        "    return 0 " +
                        "end";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(renewScript);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(
                script,
                Collections.singletonList(fullKey),
                lockId,
                String.valueOf(expireTime.getSeconds())
        );

        return RELEASE_SUCCESS.equals(result);
    }

    /**
     * 释放锁并记录日志
     *
     * @param lockKey 锁的key
     * @param lockId 锁的唯一标识
     */
    private void releaseLock(String lockKey, String lockId) {
        boolean unlocked = unlock(lockKey, lockId);
        if (!unlocked) {
            log.warn("Failed to release lock: {}", lockKey);
        }
    }
}
