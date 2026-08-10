package com.vebcoding.trade.task.mapper;

import com.vebcoding.trade.task.api.TaskView;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static com.vebcoding.trade.common.JdbcValueSupport.isoToTimestamp;

@Repository
public class JdbcTaskMapper implements TaskMapper {
    private final JdbcTemplate jdbcTemplate;

    public JdbcTaskMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TaskView> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, title, priority, status, due_at, related_type, related_id
                FROM followup_tasks
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                """, (rs, rowNum) -> new TaskView(
                rs.getString("id"),
                rs.getString("tenant_id"),
                rs.getString("title"),
                rs.getString("priority"),
                rs.getString("status"),
                dueAtToIso(rs.getTimestamp("due_at")), value(rs.getString("related_type")),
                value(rs.getString("related_id"))), tenantId);
    }

    @Override
    public Optional<TaskView> findByTenantIdAndId(String tenantId, String id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, tenant_id, title, priority, status, due_at, related_type, related_id
                    FROM followup_tasks
                    WHERE tenant_id = ? AND id = ?
                    """, (rs, rowNum) -> new TaskView(
                    rs.getString("id"),
                    rs.getString("tenant_id"),
                    rs.getString("title"),
                    rs.getString("priority"),
                    rs.getString("status"),
                    dueAtToIso(rs.getTimestamp("due_at")), value(rs.getString("related_type")),
                    value(rs.getString("related_id"))), tenantId, id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public TaskView save(TaskView task) {
        jdbcTemplate.update("""
                INSERT INTO followup_tasks (id, tenant_id, title, priority, status, due_at, related_type, related_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  title = VALUES(title),
                  priority = VALUES(priority),
                  status = VALUES(status),
                  due_at = VALUES(due_at),
                  related_type = VALUES(related_type),
                  related_id = VALUES(related_id)
                """,
                task.id(),
                task.tenantId(),
                task.title(),
                task.priority(),
                task.status(),
                isoToTimestamp(task.dueAt()),
                task.relatedType(),
                task.relatedId());
        return task;
    }

    private String dueAtToIso(Timestamp dueAt) {
        return dueAt == null ? "" : dueAt.toLocalDateTime().toString();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
