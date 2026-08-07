package com.vebcoding.trade.inquiry.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public TopicExchange tradeAiExchange() {
        return new TopicExchange("trade.ai", true, false);
    }
}