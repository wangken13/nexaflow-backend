package com.vebcoding.trade.common;

import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcSessionVerifier implements SessionVerifier {
    private final JdbcTemplate jdbcTemplate;

    JdbcSessionVerifier(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Override
    public boolean isActive(String sessionId, String userId, String tenantId, String role) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auth_user_sessions s
                JOIN users u ON u.id = s.user_id
                WHERE s.id = ? AND s.user_id = ? AND s.revoked_at IS NULL AND s.expires_at > CURRENT_TIMESTAMP
                  AND u.status = 'ACTIVE' AND u.tenant_id = ? AND u.role_code = ?
                """, Integer.class, sessionId, userId, tenantId, role);
        return count != null && count == 1;
    }
}
