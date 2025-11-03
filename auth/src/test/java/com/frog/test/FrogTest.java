package com.frog.test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基础测试类示例
 *
 * @author Deng
 * createData 2025/11/3 10:07
 */
@SpringBootTest
@ImportAutoConfiguration
public class FrogTest {

    @Test
    public void testExample() {
        assertTrue(true, "这是一个示例测试");
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
    }
}
