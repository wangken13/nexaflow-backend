package com.vebcoding.trade.inquiry.service;

import com.vebcoding.trade.inquiry.api.InquiryCreatedEvent;
import com.vebcoding.trade.inquiry.api.InquiryView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitInquiryEventPublisher implements InquiryEventPublisher {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public RabbitInquiryEventPublisher(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishCreated(InquiryView inquiry) {
        InquiryCreatedEvent event = new InquiryCreatedEvent("evt-" + UUID.randomUUID(), inquiry.id(), inquiry.tenantId(), inquiry.content());
        try {
            jdbcTemplate.update("""
                    INSERT INTO integration_outbox (id, aggregate_id, tenant_id, event_type, payload_json)
                    VALUES (?, ?, ?, 'INQUIRY_CREATED', ?)
                    """, event.eventId(), inquiry.id(), inquiry.tenantId(), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化询盘事件", exception);
        }
    }
}
