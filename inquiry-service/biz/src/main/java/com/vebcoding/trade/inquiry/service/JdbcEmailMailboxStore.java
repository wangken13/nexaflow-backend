package com.vebcoding.trade.inquiry.service;

import static com.vebcoding.trade.common.JdbcValueSupport.stringOrEmpty;
import static com.vebcoding.trade.common.JdbcValueSupport.timestampToIso;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcEmailMailboxStore implements EmailMailboxStore {
    private static final String COLUMNS = """
            id, tenant_id, display_name, email_address, imap_host, imap_port, username, encrypted_password,
            folder_name, active_flag, connection_status, last_sync_at, last_error, created_at
            """;
    private final JdbcTemplate jdbcTemplate;

    JdbcEmailMailboxStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public EmailMailbox save(EmailMailbox mailbox) {
        jdbcTemplate.update("""
                INSERT INTO email_mailbox_configs
                  (id, tenant_id, display_name, email_address, imap_host, imap_port, username,
                   encrypted_password, folder_name, active_flag, connection_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, mailbox.id(), mailbox.tenantId(), mailbox.displayName(), mailbox.emailAddress(), mailbox.host(),
                mailbox.port(), mailbox.username(), mailbox.encryptedPassword(), mailbox.folder(), mailbox.active(),
                mailbox.connectionStatus());
        return mailbox;
    }

    @Override public List<EmailMailbox> findByTenant(String tenantId) {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM email_mailbox_configs WHERE tenant_id=? ORDER BY created_at DESC",
                (rs, rowNum) -> map(rs), tenantId);
    }

    @Override public List<EmailMailbox> findActive() {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM email_mailbox_configs WHERE active_flag=1",
                (rs, rowNum) -> map(rs));
    }

    @Override public Optional<EmailMailbox> findByTenantAndId(String tenantId, String id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("SELECT " + COLUMNS
                    + " FROM email_mailbox_configs WHERE tenant_id=? AND id=?", (rs, rowNum) -> map(rs), tenantId, id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override public boolean disable(String tenantId, String id) {
        return jdbcTemplate.update("UPDATE email_mailbox_configs SET active_flag=0 WHERE tenant_id=? AND id=? AND active_flag=1",
                tenantId, id) > 0;
    }

    @Override public void recordSuccess(String id) {
        jdbcTemplate.update("""
                UPDATE email_mailbox_configs SET connection_status='CONNECTED', last_sync_at=NOW(), last_error=NULL
                WHERE id=?
                """, id);
    }

    @Override public void recordFailure(String id, String message) {
        jdbcTemplate.update("""
                UPDATE email_mailbox_configs SET connection_status='ERROR', last_error=? WHERE id=?
                """, message == null ? "邮箱同步失败" : message.substring(0, Math.min(message.length(), 500)), id);
    }

    private EmailMailbox map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new EmailMailbox(rs.getString("id"), rs.getString("tenant_id"), rs.getString("display_name"),
                rs.getString("email_address"), rs.getString("imap_host"), rs.getInt("imap_port"),
                rs.getString("username"), rs.getString("encrypted_password"), rs.getString("folder_name"),
                rs.getBoolean("active_flag"), rs.getString("connection_status"), timestampToIso(rs, "last_sync_at"),
                stringOrEmpty(rs, "last_error"), timestampToIso(rs, "created_at"));
    }
}
