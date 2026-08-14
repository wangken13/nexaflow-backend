package com.vebcoding.trade.task.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcOperationalMetricsProvider implements OperationalMetricsProvider {
    private final JdbcTemplate jdbcTemplate;

    public JdbcOperationalMetricsProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Metrics load(String tenantId) {
        return new Metrics(
                count("SELECT COUNT(*) FROM inquiries WHERE tenant_id=? AND created_at >= CURRENT_DATE", tenantId),
                count("SELECT COUNT(*) FROM trade_orders WHERE tenant_id=? AND risk=1 AND status NOT IN ('DELIVERED','CANCELLED')", tenantId),
                count("SELECT COUNT(*) FROM quotations WHERE tenant_id=? AND status='PENDING_APPROVAL'", tenantId),
                count("SELECT COUNT(*) FROM followup_tasks WHERE tenant_id=? AND status<>'DONE' AND due_at < NOW()", tenantId));
    }

    private int count(String sql, String tenantId) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, tenantId);
        return result == null ? 0 : result;
    }
}
