package com.vebcoding.trade.ai.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public Queue aiAnalysisQueue() {
        return new Queue("trade.ai.analysis", true);
    }
}