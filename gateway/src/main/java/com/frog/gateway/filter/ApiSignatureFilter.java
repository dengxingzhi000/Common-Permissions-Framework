package com.frog.gateway.filter;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.frog.common.response.ApiResponse;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * API签名验证过滤器（防重放攻击）
 *
 * @author Deng
 * createData 2025/10/24 14:48
 * @version 1.0
 */
@Component
@Slf4j
public class ApiSignatureFilter implements GlobalFilter, Ordered {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    // 签名密钥（生产环境应从配置中心获取）
    private static final String SECRET_KEY = "your-api-secret-key-2024";
    // 时间戳有效期（5分钟）
    private static final long TIMESTAMP_VALIDITY = 5 * 60 * 1000;

    // 白名单路径
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/public"
    );

    public ApiSignatureFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 白名单跳过
        if (WHITE_LIST.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 获取签名相关Header
        String timestamp = request.getHeaders().getFirst("X-Timestamp");
        String nonce = request.getHeaders().getFirst("X-Nonce");
        String signature = request.getHeaders().getFirst("X-Signature");
        String appId = request.getHeaders().getFirst("X-App-Id");

        // 验证必填参数
        if (timestamp == null || nonce == null || signature == null || appId == null) {
            return unauthorized(exchange, "缺少签名参数");
        }

        try {
            // 1. 验证时间戳（防重放）
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            if (Math.abs(currentTime - requestTime) > TIMESTAMP_VALIDITY) {
                return unauthorized(exchange, "请求已过期");
            }

            // 2. 验证nonce（防重放）
            String nonceKey = "api:nonce:" + nonce;
            return redisTemplate.hasKey(nonceKey)
                    .flatMap(exists -> {
                        if (Boolean.TRUE.equals(exists)) {
                            return unauthorized(exchange, "请求已被处理");
                        }

                        // 3. 验证签名
                        String expectedSignature = calculateSignature(request, timestamp, nonce, appId);
                        if (!signature.equals(expectedSignature)) {
                            log.warn("Signature verification failed. AppId: {}, Path: {}", appId, path);
                            return unauthorized(exchange, "签名验证失败");
                        }

                        // 4. 记录nonce（有效期内防重放）
                        return redisTemplate.opsForValue()
                                .set(nonceKey, "1", Duration.ofMillis(TIMESTAMP_VALIDITY))
                                .then(chain.filter(exchange));
                    });

        } catch (Exception e) {
            log.error("Signature validation error", e);
            return unauthorized(exchange, "签名验证异常");
        }
    }

    /**
     * 计算签名
     * 算法: HMAC-SHA256(timestamp + nonce + appId + uri + sortedParams, SECRET_KEY)
     */
    private String calculateSignature(ServerHttpRequest request, String timestamp,
                                      String nonce, String appId) {
        // 1. 获取请求URI
        String uri = request.getURI().getPath();

        // 2. 获取并排序查询参数
        Map<String, String> params = request.getQueryParams().toSingleValueMap();
        String sortedParams = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        // 3. 构建待签名字符串
        String signContent = timestamp + nonce + appId + uri + sortedParams;

        // 4. 使用HMAC-SHA256计算签名
        return SecureUtil.hmac(HmacAlgorithm.HmacSHA256, SECRET_KEY)
                .digestHex(signContent);
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");

        ApiResponse<Void> result = ApiResponse.fail(401, message);
        DataBuffer buffer = response.bufferFactory()
                .wrap(JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -50; // 在认证过滤器之前执行
    }
}
