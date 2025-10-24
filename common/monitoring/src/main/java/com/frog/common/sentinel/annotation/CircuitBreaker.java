package com.frog.common.sentinel.annotation;

import java.lang.annotation.*;

/**
 * 熔断降级注解
 *
 * @author Deng
 * createData 2025/10/21 16:11
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CircuitBreaker {
    /**
     * 资源名称
     */
    String value() default "";

    /**
     * 降级策略：0-慢调用比例 1-异常比例 2-异常数
     */
    int strategy() default 0;

    /**
     * 阈值
     */
    double threshold() default 0.5;

    /**
     * 时间窗口（秒）
     */
    int timeWindow() default 10;

    /**
     * 最小请求数
     */
    int minRequestAmount() default 5;
}