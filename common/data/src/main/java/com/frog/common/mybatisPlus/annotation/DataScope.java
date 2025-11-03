package com.frog.common.mybatisPlus.annotation;

import java.lang.annotation.*;
/**
 * 数据权限注解
 * 用于标记需要进行数据权限过滤的方法
 *
 * @author Deng
 * createData 2025/10/30 11:16
 * @version 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    /**
     * 部门表的别名
     */
    String deptAlias() default "dept_id";

    /**
     * 用户表的别名
     */
    String userAlias() default "create_by";

    /**
     * 是否启用数据权限
     */
    boolean enabled() default true;
}
