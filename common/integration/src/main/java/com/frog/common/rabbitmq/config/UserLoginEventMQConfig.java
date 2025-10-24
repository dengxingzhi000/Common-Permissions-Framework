package com.frog.common.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 *
 * @author Deng
 * createData 2025/10/24 13:59
 * @version 1.0
 */
@Configuration
public class UserLoginEventMQConfig {
    public static final String EXCHANGE = "user.login.exchange";
    public static final String QUEUE = "user.login.queue";
    public static final String ROUTING_KEY = "user.login.key";

    /**
     * 创建直连交换机
     */
    @Bean
    public DirectExchange userLoginExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    /**
     * 创建队列
     */
    @Bean
    public Queue userLoginQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    /**
     * 绑定交换机和队列
     */
    @Bean
    public Binding userLoginBinding(Queue userLoginQueue, DirectExchange userLoginExchange) {
        return BindingBuilder.bind(userLoginQueue)
                .to(userLoginExchange)
                .with(ROUTING_KEY);
    }
}
