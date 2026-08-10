package com.vebcoding.trade.auth.mapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWechatOAuthMapper implements WechatOAuthMapper {
    private final JdbcTemplate jdbcTemplate;

    public JdbcWechatOAuthMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveState(String state, Instant expiresAt) {
        jdbcTemplate.update("INSERT INTO auth_oauth_states (state, provider, expires_at) VALUES (?, 'WECHAT', ?)",
                state, Timestamp.from(expiresAt));
    }

    @Override
    public boolean consumeState(String state) {
        return jdbcTemplate.update("""
                UPDATE auth_oauth_states
                SET consumed_at = CURRENT_TIMESTAMP
                WHERE state = ? AND provider = 'WECHAT' AND consumed_at IS NULL AND expires_at > CURRENT_TIMESTAMP
                """, state) == 1;
    }

    @Override
    public Optional<AuthMapper.UserAccount> findActiveUserByIdentity(String provider, String providerSubject) {
        return jdbcTemplate.query("""
                SELECT user.id, user.username, user.password_hash, user.tenant_id, user.role_code, user.display_name, user.phone
                FROM auth_external_identities identity_map
                JOIN users user ON user.id = identity_map.user_id
                WHERE identity_map.provider = ? AND identity_map.provider_subject = ? AND user.status = 'ACTIVE'
                LIMIT 1
                """, this::toUser, provider, providerSubject).stream().findFirst();
    }

    @Override
    public void saveLoginTicket(String ticket, String userId, Instant expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO auth_wechat_login_tickets (id, user_id, expires_at)
                VALUES (?, ?, ?)
                """, ticket, userId, Timestamp.from(expiresAt));
    }

    @Override
    public Optional<AuthMapper.UserAccount> findActiveUserByTicket(String ticket, Instant now) {
        return jdbcTemplate.query("""
                SELECT user.id, user.username, user.password_hash, user.tenant_id, user.role_code, user.display_name, user.phone
                FROM auth_wechat_login_tickets ticket
                JOIN users user ON user.id = ticket.user_id
                WHERE ticket.id = ? AND ticket.consumed_at IS NULL AND ticket.expires_at > ? AND user.status = 'ACTIVE'
                LIMIT 1
                """, this::toUser, ticket, Timestamp.from(now)).stream().findFirst();
    }

    @Override
    public boolean consumeLoginTicket(String ticket) {
        return jdbcTemplate.update("""
                UPDATE auth_wechat_login_tickets
                SET consumed_at = CURRENT_TIMESTAMP
                WHERE id = ? AND consumed_at IS NULL AND expires_at > CURRENT_TIMESTAMP
                """, ticket) == 1;
    }

    @Override
    public void purgeExpired(Instant before) {
        Timestamp cutoff = Timestamp.from(before);
        jdbcTemplate.update("DELETE FROM auth_oauth_states WHERE expires_at < ?", cutoff);
        jdbcTemplate.update("DELETE FROM auth_wechat_login_tickets WHERE expires_at < ?", cutoff);
    }

    private AuthMapper.UserAccount toUser(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new AuthMapper.UserAccount(rs.getString("id"), rs.getString("username"), rs.getString("password_hash"),
                rs.getString("tenant_id"), rs.getString("role_code"), rs.getString("display_name"), rs.getString("phone"));
    }
}
