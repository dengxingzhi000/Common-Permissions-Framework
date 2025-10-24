package com.frog.common.trace.util;

import org.apache.skywalking.apm.toolkit.trace.CallableWrapper;
import org.apache.skywalking.apm.toolkit.trace.RunnableWrapper;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;

import java.util.concurrent.Callable;

/**
 * SkyWalking工具类
 *
 * @author Deng
 * createData 2025/10/21 16:29
 * @version 1.0
 */
public class TraceUtils {
    /**
     * 获取TraceId
     */
    public static String getTraceId() {
        return TraceContext.traceId();
    }

    /**
     * 包装Runnable以传递TraceContext
     */
    public static Runnable wrapRunnable(Runnable runnable) {
        return RunnableWrapper.of(runnable);
    }

    /**
     * 包装Callable以传递TraceContext
     */
    public static <V> Callable<V> wrapCallable(Callable<V> callable) {
        return CallableWrapper.of(callable);
    }
}