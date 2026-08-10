package com.vebcoding.trade.inquiry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vebcoding.trade.inquiry.api.InquiryCreatedEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public OutboxDispatcher(JdbcTemplate jdbcTemplate, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.dispatch-delay-ms:5000}")
    public void dispatch() {
        List<OutboxEvent> events = jdbcTemplate.query("""
                SELECT id, payload_json FROM integration_outbox
                WHERE status = 'PENDING' AND next_attempt_at <= CURRENT_TIMESTAMP
                ORDER BY created_at LIMIT 50
                """, (rs, rowNum) -> new OutboxEvent(rs.getString("id"), rs.getString("payload_json")));
        for (OutboxEvent event : events) dispatch(event);
    }

    private void dispatch(OutboxEvent event) {
        // Conditional state transition prevents competing instances from publishing the same row concurrently.
        if (!claim(event.id())) {
            return;
        }
        try {
            InquiryCreatedEvent payload = objectMapper.readValue(event.payload(), InquiryCreatedEvent.class);
            rabbitTemplate.convertAndSend("trade.ai", "ai.analysis.requested", payload);
            jdbcTemplate.update("UPDATE integration_outbox SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP WHERE id = ?", event.id());
            log.info("outbox.event.published eventId={}", event.id());
        } catch (Exception exception) {
            jdbcTemplate.update("""
                    UPDATE integration_outbox SET status = 'PENDING', next_attempt_at = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 SECOND)
                    WHERE id = ?
                    """, event.id());
            log.warn("outbox.event.retry_scheduled eventId={}", event.id(), exception);
        }
    }

    private boolean claim(String eventId) {
        return jdbcTemplate.update("""
                UPDATE integration_outbox
                SET status = 'PUBLISHING', attempts = attempts + 1
                WHERE id = ? AND status = 'PENDING'
                """, eventId) == 1;
    }

    private record OutboxEvent(String id, String payload) { }
}
