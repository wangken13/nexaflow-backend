package com.vebcoding.trade.task.config;

import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean public Queue inquiryTaskQueue() {
        return new Queue("trade.task.inquiry", true, false, false, Map.of("x-dead-letter-exchange", "trade.task.dlx"));
    }
    @Bean public Queue inquiryTaskDeadLetterQueue() { return new Queue("trade.task.inquiry.dlq", true); }
    @Bean public TopicExchange tradeEventsExchange() { return new TopicExchange("trade.events", true, false); }
    @Bean public TopicExchange taskDeadLetterExchange() { return new TopicExchange("trade.task.dlx", true, false); }
    @Bean public Binding inquiryTaskBinding(Queue inquiryTaskQueue, TopicExchange tradeEventsExchange) {
        return BindingBuilder.bind(inquiryTaskQueue).to(tradeEventsExchange).with("inquiry.created");
    }
    @Bean public Binding inquiryTaskDeadLetterBinding(Queue inquiryTaskDeadLetterQueue,
                                                       TopicExchange taskDeadLetterExchange) {
        return BindingBuilder.bind(inquiryTaskDeadLetterQueue).to(taskDeadLetterExchange).with("#");
    }
    @Bean public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }
}
