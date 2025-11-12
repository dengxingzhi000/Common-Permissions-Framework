package com.frog.gateway.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Deng
 * createData 2025/11/11 9:20
 * @version 1.0
 */
@Component
public class HmacSha256V2WithBody implements SignatureAlgorithm {
    @Override
    public String version() {
        return "HMAC-SHA256-V2";
    }

    @Override
    public Mono<String> calculate(ServerHttpRequest request, String appId, String timestamp,
                                  String nonce, String secretKey) {
        return DataBufferUtils.join(request.getBody())
                .defaultIfEmpty(new DefaultDataBufferFactory().wrap(new byte[0]))
                .map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    String body = new String(bytes, StandardCharsets.UTF_8);
                    String bodyHash = DigestUtils.sha256Hex(body);

                    String signContent = timestamp + nonce + appId +
                            request.getURI().getPath() +
                            sortQueryParams(request) +
                            bodyHash;

                    return SecureUtil.hmac(HmacAlgorithm.HmacSHA256, secretKey).digestHex(signContent);
                });
    }

    private String sortQueryParams(ServerHttpRequest request) {
        return request.getQueryParams().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + String.join(",", e.getValue()))
                .collect(Collectors.joining("&"));
    }

    @Override
    public Mono<Boolean> verify(ServerHttpRequest request, String signature, String appId,
                                String timestamp, String nonce, String secretKey) {
        return calculate(request, appId, timestamp, nonce, secretKey)
                .map(calculated -> MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8),
                        calculated.getBytes(StandardCharsets.UTF_8)));
    }
}
