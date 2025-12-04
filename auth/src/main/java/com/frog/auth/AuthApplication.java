package com.frog.auth;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 认证服务
 *
 * @author Deng
 * createData 2025/10/11 10:16
 * @version 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@MapperScan({"com.frog.common.log.mapper", "com.frog.auth.mapper"})
@EnableTransactionManagement
@EnableCaching
@EnableScheduling
@ComponentScan(basePackages = {"com.frog", "com.frog.common", "com.frog.auth"})
@EnableFeignClients(basePackages = "com.frog.common.feign.client")
@EnableDubbo(scanBasePackages = "com.frog")
public class AuthApplication {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
