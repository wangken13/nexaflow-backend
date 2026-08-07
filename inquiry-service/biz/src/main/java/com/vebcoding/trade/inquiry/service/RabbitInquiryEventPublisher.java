package com.vebcoding.trade.inquiry.service;

import com.vebcoding.trade.inquiry.api.InquiryCreatedEvent;
import com.vebcoding.trade.inquiry.api.InquiryView;
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
    public void publishCreated(InquiryView inquiry) {
        InquiryCreatedEvent event = new InquiryCreatedEvent(inquiry.id(), inquiry.tenantId(), inquiry.content());
        rabbitTemplate.ifAvailable(template -> template.convertAndSend("trade.ai", "ai.analysis.requested", event));
    }
}