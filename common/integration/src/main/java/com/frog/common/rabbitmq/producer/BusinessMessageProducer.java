package com.frog.common.rabbitmq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 消息中间件
 *
 * @author Deng
 * createData 2025/10/24 13:47
 * @version 1.0
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessMessageProducer {
    private final RabbitTemplate rabbitTemplate;

    /**
     * 同步发送消息
     */
    public <T> void sendSync(String exchange, String routingKey, T payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            log.info("Send sync message success, exchange: {}, routingKey: {}", exchange, routingKey);
        } catch (Exception e) {
            log.error("Send sync message failed, exchange: {}", exchange, e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    /**
     * 异步发送消息（利用 ConfirmCallback + ReturnCallback 实现）
     */
    public <T> void sendAsync(String exchange, String routingKey, T payload) {
        try {
            rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
                if (ack) {
                    log.info("Send async message success, exchange: {}, routingKey: {}", exchange, routingKey);
                } else {
                    log.error("Send async message NACK, exchange: {}, cause: {}", exchange, cause);
                }
            });

            rabbitTemplate.setReturnsCallback(returned ->
                    log.error("Message returned: replyCode={}, replyText={}, exchange={}, routingKey={}",
                            returned.getReplyCode(),
                            returned.getReplyText(),
                            returned.getExchange(),
                            returned.getRoutingKey())
            );

            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } catch (Exception e) {
            log.error("Send async message error, exchange: {}", exchange, e);
        }
    }

    /**
     * 单向发送消息（不关心结果）
     */
    public <T> void sendOneWay(String exchange, String routingKey, T payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            log.debug("Send one-way message, exchange: {}", exchange);
        } catch (Exception e) {
            log.error("Send one-way message failed, exchange: {}", exchange, e);
        }
    }

    /**
     * 发送延迟消息（基于 x-delayed-message 插件 或 TTL + DLX）
     * 注意：RabbitMQ 默认不支持 delayLevel，需要启用延迟插件
     */
    public <T> void sendDelayed(String exchange, String routingKey, T payload, int delayMillis) {
        try {
            Message message = MessageBuilder
                    .withBody(payload.toString().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN)
                    .setHeader("x-delay", delayMillis)
                    .build();
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("Send delayed message success, exchange: {}, delay: {}ms", exchange, delayMillis);
        } catch (Exception e) {
            log.error("Send delayed message failed, exchange: {}", exchange, e);
            throw new RuntimeException("发送延迟消息失败", e);
        }
    }

    /**
     * 顺序消息（RabbitMQ 不原生支持，需要手动分区队列）
     */
    public <T> void sendOrderly(String exchange, String routingKey, T payload, String hashKey) {
        try {
            // 用 hashKey 决定路由键或队列名，确保相同 hashKey 的消息进入同一队列
            String orderedRoutingKey = routingKey + "." + (Math.abs(hashKey.hashCode()) % 4);
            rabbitTemplate.convertAndSend(exchange, orderedRoutingKey, payload);
            log.info("Send orderly message success, exchange: {}, routingKey: {}", exchange, orderedRoutingKey);
        } catch (Exception e) {
            log.error("Send orderly message failed, exchange: {}", exchange, e);
            throw new RuntimeException("发送顺序消息失败", e);
        }
    }

    /**
     * 模拟事务消息（RabbitMQ 支持事务通道或确认机制）
     * 推荐使用发布确认机制代替事务，事务性能较低
     */
    public <T> void sendInTransaction(String exchange, String routingKey, T payload) {
        try {
            rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(exchange, routingKey, payload);
                if (Math.random() > 0.5) { // 模拟业务异常
                    throw new RuntimeException("事务模拟失败");
                }
                operations.waitForConfirmsOrDie(5000);
                return true;
            });
            log.info("Send transaction message success, exchange: {}", exchange);
        } catch (Exception e) {
            log.error("Send transaction message failed, exchange: {}", exchange, e);
            throw new RuntimeException("发送事务消息失败", e);
        }
    }
}
