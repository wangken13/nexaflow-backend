package com.vebcoding.trade.tenant.mapper;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.tenant.api.TenantProfileResponse;
import com.vebcoding.trade.tenant.api.AuditLogView;
import com.vebcoding.trade.tenant.api.MemberView;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTenantMapper implements TenantMapper {
    private final JdbcTemplate jdbcTemplate;

    public JdbcTenantMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TenantProfileResponse findProfile(String tenantId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT id, name, plan_code
                    FROM tenants
                    WHERE id = ?
                    """, (rs, rowNum) -> new TenantProfileResponse(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("plan_code"),
                    0,
                    3000), tenantId);
        } catch (EmptyResultDataAccessException ex) {
            throw BusinessException.notFound("企业信息不存在");
        }
    }

    @Override
    public List<MemberView> findMembers(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, username, display_name, email, role_code, status, created_at
                FROM users WHERE tenant_id = ? ORDER BY created_at
                """, (rs, rowNum) -> member(rs), tenantId);
    }

    @Override
    public Optional<MemberView> findMember(String tenantId, String id) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, username, display_name, email, role_code, status, created_at
                FROM users WHERE tenant_id = ? AND id = ?
                """, (rs, rowNum) -> member(rs), tenantId, id).stream().findFirst();
    }

    @Override
    public MemberView saveMember(MemberView member, String passwordHash) {
        jdbcTemplate.update("""
                INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, role_code, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, member.id(), member.tenantId(), member.username(), passwordHash, member.displayName(),
                member.email(), member.role(), member.status(), Timestamp.from(java.time.Instant.parse(member.createdAt())));
        return member;
    }

    @Override
    public MemberView updateMemberRole(String tenantId, String id, String role) {
        jdbcTemplate.update("UPDATE users SET role_code = ?, updated_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND id = ?",
                role, tenantId, id);
        return findMember(tenantId, id).orElseThrow(() -> BusinessException.notFound("成员不存在"));
    }

    @Override
    public MemberView updateMemberStatus(String tenantId, String id, String status) {
        jdbcTemplate.update("UPDATE users SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND id = ?",
                status, tenantId, id);
        return findMember(tenantId, id).orElseThrow(() -> BusinessException.notFound("成员不存在"));
    }

    @Override
    public List<AuditLogView> findAuditLogs(String tenantId, String module, String keyword) {
        String moduleFilter = module == null ? "" : module.trim();
        String textFilter = keyword == null ? "" : keyword.trim();
        return jdbcTemplate.query("""
                SELECT id, tenant_id, actor, module, action, target_id, detail, created_at
                FROM audit_logs
                WHERE tenant_id = ?
                  AND (? = '' OR module = ?)
                  AND (? = '' OR actor LIKE CONCAT('%', ?, '%') OR detail LIKE CONCAT('%', ?, '%'))
                ORDER BY created_at DESC LIMIT 200
                """, (rs, rowNum) -> new AuditLogView(rs.getString("id"), rs.getString("tenant_id"),
                rs.getString("actor"), rs.getString("module"), rs.getString("action"),
                rs.getString("target_id"), rs.getString("detail"), rs.getTimestamp("created_at").toInstant().toString()),
                tenantId, moduleFilter, moduleFilter, textFilter, textFilter, textFilter);
    }

    @Override
    public AuditLogView saveAuditLog(AuditLogView auditLog) {
        jdbcTemplate.update("""
                INSERT INTO audit_logs (id, tenant_id, actor, module, action, target_id, detail, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, auditLog.id(), auditLog.tenantId(), auditLog.actor(), auditLog.module(), auditLog.action(),
                auditLog.targetId(), auditLog.detail(), Timestamp.from(java.time.Instant.parse(auditLog.createdAt())));
        return auditLog;
    }

    private MemberView member(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new MemberView(rs.getString("id"), rs.getString("tenant_id"), rs.getString("username"),
                rs.getString("display_name"), rs.getString("email"), rs.getString("role_code"),
                rs.getString("status"), rs.getTimestamp("created_at").toInstant().toString());
    }
}
