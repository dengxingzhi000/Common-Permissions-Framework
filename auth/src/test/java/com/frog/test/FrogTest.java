package com.frog.test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 基础测试类示例
 *
 * @author Deng
 * createData 2025/11/3 10:07
 */
@SpringBootTest
@ImportAutoConfiguration
public class FrogTest {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    public void testExample() {
        System.out.println("passwordEncoder = " +
                passwordEncoder.matches("D123456", "$2a$10$IksNdaP/LeACZS9H/FATbOOvHVSieAuYurijkhZhJl1r.b14.IUDC"));

        // 查看加密后的数据
        String rawPassword = "Greenplate3$$!";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Encoded password: " + encodedPassword);
        
        // 验证加密后的密码是否能正确匹配原始密码
        boolean isMatch = passwordEncoder.matches(rawPassword, encodedPassword);
        System.out.println("Password match result: " + isMatch);
    }

    @Test
    public void testAnotherExample() {
        String str = "Frog";
        assertEquals("Frog", str, "字符串应该相等");
        System.out.println("str = " + str);
    }

    private void assertEquals(String expected, String actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message);
        }
    }

    @Configuration
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
        
        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
