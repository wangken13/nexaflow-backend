package com.vebcoding.trade.common;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcImportJobRecorder implements ImportJobRecorder {
    private final JdbcTemplate jdbcTemplate;

    public JdbcImportJobRecorder(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public String start(String resourceType, int received) {
        String id = "imp-" + UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO data_import_jobs
                  (id, tenant_id, operator_id, resource_type, status, received_count, created_at)
                VALUES (?, ?, ?, ?, 'PROCESSING', ?, ?)
                """, id, TenantContext.tenantId(), TenantContext.userId(), resourceType, received,
                Timestamp.from(Instant.now()));
        return id;
    }

    @Transactional
    public BulkImportResult complete(String jobId, int received, int imported, List<String> errors) {
        for (int index = 0; index < errors.size(); index++) {
            String message = errors.get(index);
            jdbcTemplate.update("""
                    INSERT INTO data_import_errors (tenant_id, job_id, `row_number`, error_message)
                    VALUES (?, ?, ?, ?)
                    """, TenantContext.tenantId(), jobId, rowNumber(message, index + 1), limit(message));
        }
        int skipped = received - imported;
        jdbcTemplate.update("""
                UPDATE data_import_jobs SET status=?, imported_count=?, skipped_count=?, completed_at=?
                WHERE tenant_id=? AND id=?
                """, errors.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_ERRORS", imported, skipped,
                Timestamp.from(Instant.now()), TenantContext.tenantId(), jobId);
        return new BulkImportResult(jobId, received, imported, skipped, List.copyOf(errors));
    }

    private int rowNumber(String message, int fallback) {
        if (message != null && message.startsWith("第")) {
            int end = message.indexOf("行");
            if (end > 1) try { return Integer.parseInt(message.substring(1, end)); } catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private String limit(String message) {
        if (message == null) return "未知导入错误";
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
