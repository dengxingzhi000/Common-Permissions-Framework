package com.frog.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

/**
 * 网关服务
 *
 * @author Deng
 * createData 2025/9/30 15:38
 * @version 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableDubbo
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
