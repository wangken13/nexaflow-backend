package com.vebcoding.trade.auth.mapper;

import java.util.Optional;
import java.util.UUID;
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
        String sql = """
                SELECT username, password_hash, tenant_id, role_code
                FROM users
                WHERE username = ?
                ORDER BY created_at DESC
                LIMIT 1
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new UserAccount(
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("tenant_id"),
                rs.getString("role_code")
        ), username).stream().findFirst();
    }

    @Override
    @Transactional
    public UserAccount createUser(String tenantName, String username, String password) {
        String tenantId = "tenant-" + UUID.randomUUID();
        String userId = "user-" + UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, plan_code) VALUES (?, ?, ?)",
                tenantId, tenantName, "PRO");
        jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, username, password_hash, role_code) VALUES (?, ?, ?, ?, ?)",
                userId, tenantId, username, password, "OWNER");

        return new UserAccount(username, password, tenantId, "OWNER");
    }
}
