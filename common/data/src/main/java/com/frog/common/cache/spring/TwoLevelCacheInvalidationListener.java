package com.frog.common.cache.spring;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class TwoLevelCacheInvalidationListener implements MessageListener {
    private final TwoLevelCacheManager cacheManager;

    @Override
    public void onMessage(@NonNull Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            if (body.length() >= 2 && body.startsWith("\"") && body.endsWith("\"")) {
                body = body.substring(1, body.length() - 1);
            }
            // 格式: cacheName|key 或 cacheName|*
            int idx = body.indexOf('|');
            if (idx > 0) {
                String cacheName = body.substring(0, idx);
                String key = body.substring(idx + 1);
                cacheManager.invalidateLocal(cacheName, key);
                log.debug("TwoLevel local invalidated: {} -> {}", cacheName, key);
            }
        } catch (Exception e) {
            log.warn("TwoLevel cache invalidation message error", e);
        }
    }
}

