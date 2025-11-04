package com.frog.common.trace.annotation;

import java.lang.annotation.*;

/**
 * 业务追踪注解
 *
 * @author Deng
 * createData 2025/10/21 16:27
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface BusinessTrace {
    /**
     * 操作名称
     */
    String operationName() default "";

    /**
     * 是否记录参数
     */
    boolean recordArgs() default true;

    /**
     * 是否记录返回值
     */
    boolean recordResult() default false;
}
