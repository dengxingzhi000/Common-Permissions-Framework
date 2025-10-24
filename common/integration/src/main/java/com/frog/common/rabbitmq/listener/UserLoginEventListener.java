package com.frog.common.rabbitmq.listener;

import com.frog.common.rabbitmq.event.UserLoginEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 用户登录事件监听器
 *
 * @author Deng
 * createData 2025/10/24 13:57
 * @version 1.0
 */
@Slf4j
@Component
public class UserLoginEventListener {
    /**
     * 监听用户登录事件队列
     * ⚙️ 对应配置：
     *  Exchange：user.login.exchange
     *  Queue：user.login.queue
     *  RoutingKey：user.login.key
     */
    @RabbitListener(queues = "user.login.queue")
    public void onMessage(@Payload UserLoginEvent event) {
        try {
            log.info("Received user login event: userId={}, ip={}",
                    event.getUserId(), event.getIpAddress());

            // 处理登录事件
            handleLoginEvent(event);

        } catch (Exception e) {
            log.error("Handle login event failed", e);
            // RabbitMQ 默认不会自动重试业务异常（除非使用 DLX 或 Spring Retry）
            // 这里抛出 RuntimeException，可结合重试机制处理
            throw new RuntimeException("处理登录事件失败", e);
        }
    }

    private void handleLoginEvent(UserLoginEvent event) {
        // 1. 记录登录日志到审计系统
        // 2. 更新用户最后登录时间
        // 3. 检查异常登录行为
        // 4. 发送登录通知
    }
}
