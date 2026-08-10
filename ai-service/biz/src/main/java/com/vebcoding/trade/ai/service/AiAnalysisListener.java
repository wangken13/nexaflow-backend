package com.vebcoding.trade.ai.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.inquiry.api.InquiryCreatedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AiAnalysisListener {
    private final AiService aiService;
    private final JdbcTemplate jdbcTemplate;

    public AiAnalysisListener(AiService aiService, JdbcTemplate jdbcTemplate) {
        this.aiService = aiService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @RabbitListener(queues = "trade.ai.analysis")
    public void onAnalysisRequested(InquiryCreatedEvent event) {
        if (jdbcTemplate.update("INSERT IGNORE INTO message_consumption (consumer_name, event_id) VALUES ('ai-analysis', ?)", event.eventId()) != 1) return;
        try {
            TenantContext.setTenantId(event.tenantId());
            TenantContext.setUserId("system-ai");
            TenantContext.setRole("OWNER");
            aiService.analyzeInquiry(event.inquiryId(), event.content());
        } finally {
            TenantContext.clear();
        }
    }
}
