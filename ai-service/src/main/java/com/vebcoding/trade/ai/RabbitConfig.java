package com.vebcoding.trade.ai;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    TopicExchange aiExchange() {
        return new TopicExchange("trade.ai");
    }

    @Bean
    Queue aiAnalysisQueue() {
        return new Queue("trade.ai.analysis.requests", true);
    }

    @Bean
    Binding aiAnalysisBinding(TopicExchange aiExchange, Queue aiAnalysisQueue) {
        return BindingBuilder.bind(aiAnalysisQueue).to(aiExchange).with("ai.analysis.requested");
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
