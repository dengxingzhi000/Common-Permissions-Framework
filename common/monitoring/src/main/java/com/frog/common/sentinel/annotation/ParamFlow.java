package com.frog.common.sentinel.annotation;

import java.lang.annotation.*;

/**
 * 热点参数限流
 *
 * @author Deng
 * createData 2025/10/21 16:12
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ParamFlow {
    /**
     * 资源名称
     */
    String value() default "";

    /**
     * 参数索引
     */
    int paramIdx() default 0;

    /**
     * QPS阈值
     */
    int count() default 10;
}
