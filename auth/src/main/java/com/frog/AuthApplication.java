package com.frog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 认证服务
 *
 * @author Deng
 * createData 2025/10/11 10:16
 * @version 1.0
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.frog.mapper")
@EnableTransactionManagement
@EnableCaching
@EnableScheduling
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
        System.out.println("Swagger文档地址: http://localhost:8106/api/swagger-ui.html");
        System.out.println("默认管理员账号: admin");
        System.out.println("默认管理员密码: admin");
    }
}
