package com.vebcoding.trade.inquiry;

import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class RabbitInquiryEventPublisher implements InquiryEventPublisher {
    private final ObjectProvider<RabbitTemplate> rabbitTemplate;

    public RabbitInquiryEventPublisher(ObjectProvider<RabbitTemplate> rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishCreated(InquiryController.InquiryView inquiry) {
        rabbitTemplate.ifAvailable(template -> template.convertAndSend("trade.ai", "ai.analysis.requested",
                Map.of("inquiryId", inquiry.id(), "tenantId", inquiry.tenantId(), "content", inquiry.content())));
    }
}
