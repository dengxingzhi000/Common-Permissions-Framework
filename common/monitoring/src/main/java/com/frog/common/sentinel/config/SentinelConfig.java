package com.frog.common.sentinel.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frog.common.sentinel.exception.SentinelExceptionHandlerStrategy;
import com.frog.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Sentinel配置类
 *
 * @author Deng
 * createData 2025/10/20 10:39
 * @version 1.0
 */
@Configuration
@RequiredArgsConstructor
public class SentinelConfig {
    private final ObjectMapper objectMapper;
    private final List<SentinelExceptionHandlerStrategy> strategies;

    @Bean
    public BlockExceptionHandler blockExceptionHandler() {
        return (request, response, e) -> {
            ApiResponse<Void> result = strategies.stream()
                    .filter(s -> s.supports(e))
                    .findFirst()
                    .map(s -> s.handle(e))
                    .orElse(ApiResponse.fail(500, "系统限流"));

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), result);
        };
    }
}