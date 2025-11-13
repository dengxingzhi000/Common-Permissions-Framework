package com.frog.gateway.util;

import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

/**
 *
 *
 * @author Deng
 * createData 2025/11/11 9:19
 * @version 1.0
 */
public interface SignatureAlgorithm {

    String version();

    Mono<String> calculate(ServerHttpRequest request, String appId, String timestamp, String nonce, String secretKey);

    Mono<Boolean> verify(ServerHttpRequest request, String signature, String appId, String timestamp,
                         String nonce, String secretKey);
}
