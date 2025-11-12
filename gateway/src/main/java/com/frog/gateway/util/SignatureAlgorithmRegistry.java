package com.frog.gateway.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 签名算法版本控制
 *
 * @author Deng
 * createData 2025/11/11 9:18
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class SignatureAlgorithmRegistry {
    private final List<SignatureAlgorithm> algorithmList;

    private final Map<String, SignatureAlgorithm> algorithms = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 自动注册所有实现类
        for (SignatureAlgorithm algorithm : algorithmList) {
            algorithms.put(algorithm.version(), algorithm);
        }
    }

    public SignatureAlgorithm getAlgorithm(String version) {
        return algorithms.getOrDefault(version, algorithms.get("HMAC-SHA256-V1"));
    }
}
