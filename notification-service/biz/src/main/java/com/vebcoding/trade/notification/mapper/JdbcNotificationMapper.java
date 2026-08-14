package com.vebcoding.trade.notification.mapper;

import com.vebcoding.trade.notification.api.NotificationView;
import com.vebcoding.trade.notification.api.SupportMessageView;
import com.vebcoding.trade.notification.api.SupportTicketView;
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

    @Override
    public List<SupportTicketView> findTickets(String tenantId, String createdBy) {
        String actor = createdBy == null ? "" : createdBy;
        return jdbcTemplate.query("""
                SELECT id, tenant_id, created_by, category, priority, subject, description, status,
                       assigned_to, created_at, updated_at
                FROM support_tickets WHERE tenant_id=? AND (?='' OR created_by=?)
                ORDER BY updated_at DESC LIMIT 200
                """, (rs, rowNum) -> ticket(rs, List.of()), tenantId, actor, actor);
    }

    @Override
    public Optional<SupportTicketView> findTicket(String tenantId, String id) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, created_by, category, priority, subject, description, status,
                       assigned_to, created_at, updated_at
                FROM support_tickets WHERE tenant_id=? AND id=?
                """, (rs, rowNum) -> ticket(rs, findTicketMessages(tenantId, id)), tenantId, id).stream().findFirst();
    }

    @Override
    public SupportTicketView saveTicket(SupportTicketView ticket) {
        jdbcTemplate.update("""
                INSERT INTO support_tickets
                  (id, tenant_id, created_by, category, priority, subject, description, status, assigned_to,
                   created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ticket.id(), ticket.tenantId(), ticket.createdBy(), ticket.category(), ticket.priority(),
                ticket.subject(), ticket.description(), ticket.status(), emptyToNull(ticket.assignedTo()),
                isoToTimestamp(ticket.createdAt()), isoToTimestamp(ticket.updatedAt()));
        return ticket;
    }

    @Override
    public SupportMessageView saveTicketMessage(String tenantId, SupportMessageView message) {
        jdbcTemplate.update("""
                INSERT INTO support_ticket_messages (id, tenant_id, ticket_id, author_id, content, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, message.id(), tenantId, message.ticketId(), message.authorId(), message.content(),
                isoToTimestamp(message.createdAt()));
        jdbcTemplate.update("UPDATE support_tickets SET status='WAITING_SUPPORT', updated_at=CURRENT_TIMESTAMP "
                + "WHERE tenant_id=? AND id=? AND status<>'CLOSED'", tenantId, message.ticketId());
        return message;
    }

    @Override
    public List<SupportMessageView> findTicketMessages(String tenantId, String ticketId) {
        return jdbcTemplate.query("""
                SELECT id, ticket_id, author_id, content, created_at FROM support_ticket_messages
                WHERE tenant_id=? AND ticket_id=? ORDER BY created_at
                """, (rs, rowNum) -> new SupportMessageView(rs.getString("id"), rs.getString("ticket_id"),
                rs.getString("author_id"), rs.getString("content"), timestampToIso(rs, "created_at")),
                tenantId, ticketId);
    }

    @Override
    public SupportTicketView updateTicketStatus(String tenantId, String id, String status) {
        jdbcTemplate.update("UPDATE support_tickets SET status=?, updated_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND id=?",
                status, tenantId, id);
        return findTicket(tenantId, id).orElseThrow();
    }

    private SupportTicketView ticket(java.sql.ResultSet rs, List<SupportMessageView> messages)
            throws java.sql.SQLException {
        return new SupportTicketView(rs.getString("id"), rs.getString("tenant_id"), rs.getString("created_by"),
                rs.getString("category"), rs.getString("priority"), rs.getString("subject"),
                rs.getString("description"), rs.getString("status"), value(rs.getString("assigned_to")),
                timestampToIso(rs, "created_at"), timestampToIso(rs, "updated_at"), messages);
    }

    private String emptyToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private String value(String value) { return value == null ? "" : value; }
}
