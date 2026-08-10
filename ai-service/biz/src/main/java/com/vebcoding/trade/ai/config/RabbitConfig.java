package com.vebcoding.trade.ai.config;

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
    @Bean
    public Queue aiAnalysisQueue() {
        return new Queue("trade.ai.analysis", true, false, false, Map.of("x-dead-letter-exchange", "trade.ai.dlx"));
    }

    @Bean public Queue aiAnalysisDeadLetterQueue() { return new Queue("trade.ai.analysis.dlq", true); }
    @Bean public TopicExchange tradeAiExchange() { return new TopicExchange("trade.ai", true, false); }
    @Bean public TopicExchange tradeAiDeadLetterExchange() { return new TopicExchange("trade.ai.dlx", true, false); }
    @Bean public Binding aiAnalysisBinding(Queue aiAnalysisQueue, TopicExchange tradeAiExchange) { return BindingBuilder.bind(aiAnalysisQueue).to(tradeAiExchange).with("ai.analysis.requested"); }
    @Bean public Binding aiAnalysisDeadLetterBinding(Queue aiAnalysisDeadLetterQueue, TopicExchange tradeAiDeadLetterExchange) { return BindingBuilder.bind(aiAnalysisDeadLetterQueue).to(tradeAiDeadLetterExchange).with("#"); }
    @Bean public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }
}
