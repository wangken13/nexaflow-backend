package com.vebcoding.trade.auth.mapper;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcAuthMapper implements AuthMapper {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAuthMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return queryUser("""
                SELECT id, username, password_hash, tenant_id, role_code, display_name, phone
                FROM users
                WHERE username = ? AND status = 'ACTIVE'
                ORDER BY created_at DESC LIMIT 1
                """, username);
    }

    @Override
    public Optional<UserAccount> findByPhone(String phone) {
        return queryUser("""
                SELECT id, username, password_hash, tenant_id, role_code, display_name, phone
                FROM users
                WHERE phone = ? AND status = 'ACTIVE'
                ORDER BY created_at DESC LIMIT 1
                """, phone);
    }

    @Override
    public Optional<UserAccount> findById(String userId) {
        return queryUser("""
                SELECT id, username, password_hash, tenant_id, role_code, display_name, phone
                FROM users
                WHERE id = ? AND status = 'ACTIVE'
                ORDER BY created_at DESC LIMIT 1
                """, userId);
    }

    private Optional<UserAccount> queryUser(String sql, String identity) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> new UserAccount(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("tenant_id"),
                rs.getString("role_code"),
                rs.getString("display_name"),
                rs.getString("phone")
        ), identity).stream().findFirst();
    }

    @Override
    @Transactional
    public UserAccount createUser(String tenantName, String username, String password, String displayName, String phone) {
        String tenantId = "tenant-" + UUID.randomUUID();
        String userId = "user-" + UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, plan_code) VALUES (?, ?, ?)",
                tenantId, tenantName, "PRO");
        jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, username, password_hash, display_name, phone, role_code) VALUES (?, ?, ?, ?, ?, ?, ?)",
                userId, tenantId, username, password, displayName, phone, "OWNER");

        return new UserAccount(userId, username, password, tenantId, "OWNER", displayName, phone);
    }

    @Override
    public void updatePassword(String username, String password) {
        jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE username = ?", password, username);
    }

    @Override
    public int countFailedAttemptsSince(String username, Instant since) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auth_login_attempts
                WHERE username = ? AND success_flag = 0 AND attempted_at >= ?
                """, Integer.class, username, Timestamp.from(since));
        return count == null ? 0 : count;
    }

    @Override
    public void recordLoginAttempt(String username, boolean success) {
        jdbcTemplate.update("INSERT INTO auth_login_attempts (username, success_flag) VALUES (?, ?)",
                username, success);
    }

    @Override
    public int countSmsCodesSince(String phone, String purpose, Instant since) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auth_sms_codes
                WHERE phone = ? AND purpose = ? AND requested_at >= ?
                """, Integer.class, phone, purpose, Timestamp.from(since));
        return count == null ? 0 : count;
    }

    @Override
    public void invalidateActiveSmsCodes(String phone, String purpose) {
        jdbcTemplate.update("""
                UPDATE auth_sms_codes SET consumed_at = CURRENT_TIMESTAMP
                WHERE phone = ? AND purpose = ? AND consumed_at IS NULL
                """, phone, purpose);
    }

    @Override
    public void saveSmsCode(SmsCode code) {
        jdbcTemplate.update("""
                INSERT INTO auth_sms_codes (id, phone, purpose, code_hash, expires_at, attempts)
                VALUES (?, ?, ?, ?, ?, ?)
                """, code.id(), code.phone(), code.purpose(), code.codeHash(), Timestamp.from(code.expiresAt()), code.attempts());
    }

    @Override
    public Optional<SmsCode> findLatestActiveSmsCode(String phone, String purpose) {
        return jdbcTemplate.query("""
                SELECT id, phone, purpose, code_hash, expires_at, attempts
                FROM auth_sms_codes
                WHERE phone = ? AND purpose = ? AND consumed_at IS NULL
                ORDER BY requested_at DESC LIMIT 1
                """, (rs, rowNum) -> new SmsCode(rs.getString("id"), rs.getString("phone"),
                rs.getString("purpose"), rs.getString("code_hash"), rs.getTimestamp("expires_at").toInstant(),
                rs.getInt("attempts")), phone, purpose).stream().findFirst();
    }

    @Override
    public boolean consumeSmsCode(String id) {
        return jdbcTemplate.update("""
                UPDATE auth_sms_codes SET consumed_at = CURRENT_TIMESTAMP
                WHERE id = ? AND consumed_at IS NULL AND expires_at >= CURRENT_TIMESTAMP AND attempts < 5
                """, id) == 1;
    }

    @Override
    public void increaseSmsCodeAttempts(String id) {
        jdbcTemplate.update("UPDATE auth_sms_codes SET attempts = attempts + 1 WHERE id = ? AND consumed_at IS NULL", id);
    }

    @Override
    public void createSession(UserSession session) {
        jdbcTemplate.update("INSERT INTO auth_user_sessions (id, user_id, expires_at) VALUES (?, ?, ?)",
                session.id(), session.userId(), Timestamp.from(session.expiresAt()));
    }

    @Override
    public Optional<UserSession> findActiveSession(String sessionId, String userId) {
        return jdbcTemplate.query("""
                SELECT id, user_id, expires_at FROM auth_user_sessions
                WHERE id = ? AND user_id = ? AND revoked_at IS NULL AND expires_at > CURRENT_TIMESTAMP
                """, (rs, rowNum) -> new UserSession(rs.getString("id"), rs.getString("user_id"),
                rs.getTimestamp("expires_at").toInstant()), sessionId, userId).stream().findFirst();
    }

    @Override
    public void revokeSession(String sessionId, String userId) {
        jdbcTemplate.update("UPDATE auth_user_sessions SET revoked_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?",
                sessionId, userId);
    }
}
