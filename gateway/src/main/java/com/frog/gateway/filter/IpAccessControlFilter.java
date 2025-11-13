package com.frog.gateway.filter;

import com.alibaba.nacos.shaded.com.google.common.cache.Cache;
import com.alibaba.nacos.shaded.com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * IP访问控制过滤器
 *
 * @author Deng
 * createData 2025/10/24 14:54
 * @version 1.0
 */
@Component
@Slf4j
public class IpAccessControlFilter implements GlobalFilter, Ordered {
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    // 本地缓存,减少Redis查询
    private final Cache<String, Boolean> ipCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private static final String IP_WHITELIST_KEY = "security:ip:whitelist:";
    private static final String IP_BLACKLIST_KEY = "security:ip:blacklist:";

    public IpAccessControlFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = getClientIp(exchange);
        String message = "IP已被拉黑";

        // 1. 先检查本地缓存
        Boolean cached = ipCache.getIfPresent(clientIp);
        if (cached != null) {
            if (!cached) {
                return blockRequest(exchange, message);
            }
            return chain.filter(exchange);
        }

        // 2. 检查黑名单
        return redisTemplate.hasKey(IP_BLACKLIST_KEY + clientIp)
                .flatMap(inBlacklist -> {
                    if (Boolean.TRUE.equals(inBlacklist)) {
                        ipCache.put(clientIp, false);
                        log.warn("Blocked blacklisted IP: {}", clientIp);
                        return blockRequest(exchange, message);
                    }

                    // 3. 检查白名单（可选）
                    // 如果启用白名单模式,只允许白名单IP访问
                    // return checkWhitelist(clientIp, exchange, chain);

                    ipCache.put(clientIp, true);
                    return chain.filter(exchange);
                });
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(ServerWebExchange exchange) {
        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            ip = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "";
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 拦截请求
     */
    private Mono<Void> blockRequest(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100; // 最高优先级
    }
}
