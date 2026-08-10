package com.vebcoding.trade.notification.mapper;

import com.vebcoding.trade.notification.api.NotificationView;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static com.vebcoding.trade.common.JdbcValueSupport.isoToTimestamp;
import static com.vebcoding.trade.common.JdbcValueSupport.timestampToIso;

@Repository
public class JdbcNotificationMapper implements NotificationMapper {
    private final JdbcTemplate jdbcTemplate;

    public JdbcNotificationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<NotificationView> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, title, content, read_flag, created_at
                FROM notifications
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                """, (rs, rowNum) -> new NotificationView(
                rs.getString("id"),
                rs.getString("tenant_id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getBoolean("read_flag"),
                timestampToIso(rs, "created_at")), tenantId);
    }

    @Override
    public Optional<NotificationView> findByTenantIdAndId(String tenantId, String id) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, title, content, read_flag, created_at
                FROM notifications WHERE tenant_id = ? AND id = ?
                """, (rs, rowNum) -> new NotificationView(rs.getString("id"), rs.getString("tenant_id"),
                rs.getString("title"), rs.getString("content"), rs.getBoolean("read_flag"),
                timestampToIso(rs, "created_at")), tenantId, id).stream().findFirst();
    }

    @Override
    public NotificationView save(NotificationView notification) {
        Timestamp createdAt = isoToTimestamp(notification.createdAt());
        jdbcTemplate.update("""
                INSERT INTO notifications (id, tenant_id, title, content, read_flag, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  title = VALUES(title),
                  content = VALUES(content),
                  read_flag = VALUES(read_flag)
                """,
                notification.id(),
                notification.tenantId(),
                notification.title(),
                notification.content(),
                notification.read(),
                createdAt);
        return notification;
    }

    @Override
    public int markAllRead(String tenantId) {
        return jdbcTemplate.update("UPDATE notifications SET read_flag = 1 WHERE tenant_id = ? AND read_flag = 0", tenantId);
    }
}
