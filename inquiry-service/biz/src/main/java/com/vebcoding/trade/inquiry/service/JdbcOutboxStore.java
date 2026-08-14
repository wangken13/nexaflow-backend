package com.vebcoding.trade.inquiry.service;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcOutboxStore implements OutboxStore {
    private final JdbcTemplate jdbcTemplate;

    JdbcOutboxStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<OutboxEvent> findDue(int limit) {
        return jdbcTemplate.query("""
                SELECT id, payload_json FROM integration_outbox
                WHERE status IN ('PENDING', 'PUBLISHING') AND next_attempt_at <= CURRENT_TIMESTAMP
                ORDER BY created_at LIMIT ?
                """, (rs, rowNum) -> new OutboxEvent(rs.getString("id"), rs.getString("payload_json")), limit);
    }

    @Override
    public boolean claim(String eventId) {
        return jdbcTemplate.update("""
                UPDATE integration_outbox
                SET status = 'PUBLISHING', attempts = attempts + 1,
                    next_attempt_at = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 2 MINUTE)
                WHERE id = ? AND status IN ('PENDING', 'PUBLISHING') AND next_attempt_at <= CURRENT_TIMESTAMP
                """, eventId) == 1;
    }

    @Override
    public void markPublished(String eventId) {
        jdbcTemplate.update("""
                UPDATE integration_outbox SET status='PUBLISHED', published_at=CURRENT_TIMESTAMP WHERE id=?
                """, eventId);
    }

    @Override
    public void scheduleRetry(String eventId) {
        jdbcTemplate.update("""
                UPDATE integration_outbox SET status='PENDING',
                    next_attempt_at=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 SECOND) WHERE id=?
                """, eventId);
    }
}
