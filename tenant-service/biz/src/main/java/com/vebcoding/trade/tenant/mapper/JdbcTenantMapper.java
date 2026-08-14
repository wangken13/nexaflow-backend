package com.vebcoding.trade.tenant.mapper;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.tenant.api.TenantProfileResponse;
import com.vebcoding.trade.tenant.api.AuditLogView;
import com.vebcoding.trade.tenant.api.MemberView;
import com.vebcoding.trade.tenant.api.ChannelConfigView;
import com.vebcoding.trade.tenant.api.KnowledgeArticleView;
import com.vebcoding.trade.tenant.api.SubscriptionView;
import com.vebcoding.trade.tenant.api.DepartmentView;
import com.vebcoding.trade.tenant.api.ImportJobView;
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
                SELECT user.id, user.tenant_id, user.username, user.display_name, user.email,
                       user.department_id, COALESCE(department.name, '') department_name,
                       user.role_code, user.data_scope, user.status, user.created_at
                FROM users user LEFT JOIN departments department
                  ON department.tenant_id=user.tenant_id AND department.id=user.department_id
                WHERE user.tenant_id = ? ORDER BY user.created_at
                """, (rs, rowNum) -> member(rs), tenantId);
    }

    @Override
    public Optional<MemberView> findMember(String tenantId, String id) {
        return jdbcTemplate.query("""
                SELECT user.id, user.tenant_id, user.username, user.display_name, user.email,
                       user.department_id, COALESCE(department.name, '') department_name,
                       user.role_code, user.data_scope, user.status, user.created_at
                FROM users user LEFT JOIN departments department
                  ON department.tenant_id=user.tenant_id AND department.id=user.department_id
                WHERE user.tenant_id = ? AND user.id = ?
                """, (rs, rowNum) -> member(rs), tenantId, id).stream().findFirst();
    }

    @Override
    public MemberView saveMember(MemberView member, String passwordHash) {
        jdbcTemplate.update("""
                INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, department_id,
                                   role_code, data_scope, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, member.id(), member.tenantId(), member.username(), passwordHash, member.displayName(),
                emptyToNull(member.departmentId()), member.role(), member.dataScope(), member.status(),
                Timestamp.from(java.time.Instant.parse(member.createdAt())));
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
    public MemberView updateMemberAccess(String tenantId, String id, String departmentId, String dataScope) {
        jdbcTemplate.update("UPDATE users SET department_id=?, data_scope=?, updated_at=CURRENT_TIMESTAMP "
                        + "WHERE tenant_id=? AND id=?", emptyToNull(departmentId), dataScope, tenantId, id);
        return findMember(tenantId, id).orElseThrow(() -> BusinessException.notFound("成员不存在"));
    }

    @Override
    public List<DepartmentView> findDepartments(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, name, parent_id, status, created_at
                FROM departments WHERE tenant_id=? ORDER BY name
                """, (rs, rowNum) -> department(rs), tenantId);
    }

    @Override
    public Optional<DepartmentView> findDepartment(String tenantId, String id) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, name, parent_id, status, created_at
                FROM departments WHERE tenant_id=? AND id=?
                """, (rs, rowNum) -> department(rs), tenantId, id).stream().findFirst();
    }

    @Override
    public DepartmentView saveDepartment(DepartmentView department) {
        jdbcTemplate.update("""
                INSERT INTO departments (id, tenant_id, name, parent_id, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, department.id(), department.tenantId(), department.name(), emptyToNull(department.parentId()),
                department.status(), Timestamp.from(java.time.Instant.parse(department.createdAt())));
        return department;
    }

    @Override
    public DepartmentView updateDepartmentStatus(String tenantId, String id, String status) {
        jdbcTemplate.update("UPDATE departments SET status=? WHERE tenant_id=? AND id=?", status, tenantId, id);
        return findDepartment(tenantId, id).orElseThrow(() -> BusinessException.notFound("部门不存在"));
    }

    @Override
    public List<ImportJobView> findImportJobs(String tenantId) {
        return jdbcTemplate.query("""
                SELECT job.id, job.resource_type, job.status, job.received_count, job.imported_count,
                       job.skipped_count, job.operator_id, job.created_at, job.completed_at,
                       COALESCE(GROUP_CONCAT(error.error_message ORDER BY error.row_number SEPARATOR '\\n'), '') errors
                FROM data_import_jobs job
                LEFT JOIN data_import_errors error ON error.tenant_id=job.tenant_id AND error.job_id=job.id
                WHERE job.tenant_id=?
                GROUP BY job.id, job.resource_type, job.status, job.received_count, job.imported_count,
                         job.skipped_count, job.operator_id, job.created_at, job.completed_at
                ORDER BY job.created_at DESC LIMIT 100
                """, (rs, rowNum) -> {
            String errors = rs.getString("errors");
            return new ImportJobView(rs.getString("id"), rs.getString("resource_type"), rs.getString("status"),
                    rs.getInt("received_count"), rs.getInt("imported_count"), rs.getInt("skipped_count"),
                    rs.getString("operator_id"), rs.getTimestamp("created_at").toInstant().toString(),
                    rs.getTimestamp("completed_at") == null ? "" : rs.getTimestamp("completed_at").toInstant().toString(),
                    errors == null || errors.isBlank() ? List.of() : List.of(errors.split("\\n")));
        }, tenantId);
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

    @Override
    public List<KnowledgeArticleView> findKnowledgeArticles(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, title, category, content, active_flag, updated_by, created_at, updated_at
                FROM knowledge_articles WHERE tenant_id=? ORDER BY updated_at DESC
                """, (rs, rowNum) -> knowledge(rs), tenantId);
    }

    @Override
    public Optional<KnowledgeArticleView> findKnowledgeArticle(String tenantId, String id) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, title, category, content, active_flag, updated_by, created_at, updated_at
                FROM knowledge_articles WHERE tenant_id=? AND id=?
                """, (rs, rowNum) -> knowledge(rs), tenantId, id).stream().findFirst();
    }

    @Override
    public KnowledgeArticleView saveKnowledgeArticle(KnowledgeArticleView article) {
        jdbcTemplate.update("""
                INSERT INTO knowledge_articles
                  (id, tenant_id, title, category, content, active_flag, updated_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE title=VALUES(title), category=VALUES(category), content=VALUES(content),
                  active_flag=VALUES(active_flag), updated_by=VALUES(updated_by), updated_at=VALUES(updated_at)
                """, article.id(), article.tenantId(), article.title(), article.category(), article.content(),
                article.active(), article.updatedBy(), Timestamp.from(java.time.Instant.parse(article.createdAt())),
                Timestamp.from(java.time.Instant.parse(article.updatedAt())));
        return article;
    }

    @Override
    public boolean deleteKnowledgeArticle(String tenantId, String id) {
        return jdbcTemplate.update("DELETE FROM knowledge_articles WHERE tenant_id=? AND id=?", tenantId, id) == 1;
    }

    @Override
    public List<ChannelConfigView> findChannelConfigs(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id, channel_type, display_name, account_ref, enabled_flag, connection_status, updated_at
                FROM channel_configs WHERE tenant_id=? ORDER BY channel_type
                """, (rs, rowNum) -> new ChannelConfigView(rs.getString("id"), rs.getString("channel_type"),
                rs.getString("display_name"), rs.getString("account_ref"), rs.getBoolean("enabled_flag"),
                rs.getString("connection_status"), rs.getTimestamp("updated_at").toInstant().toString()), tenantId);
    }

    @Override
    public ChannelConfigView saveChannelConfig(String tenantId, ChannelConfigView channel, String updatedBy) {
        jdbcTemplate.update("""
                INSERT INTO channel_configs
                  (id, tenant_id, channel_type, display_name, account_ref, enabled_flag, connection_status, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), account_ref=VALUES(account_ref),
                  enabled_flag=VALUES(enabled_flag), connection_status=VALUES(connection_status),
                  updated_by=VALUES(updated_by), updated_at=CURRENT_TIMESTAMP
                """, channel.id(), tenantId, channel.channelType(), channel.displayName(), channel.accountRef(),
                channel.enabled(), channel.connectionStatus(), updatedBy);
        return findChannelConfigs(tenantId).stream()
                .filter(item -> item.channelType().equals(channel.channelType())).findFirst().orElseThrow();
    }

    @Override
    public SubscriptionView findSubscription(String tenantId) {
        return jdbcTemplate.queryForObject("""
                SELECT t.plan_code, p.plan_name, p.monthly_price, p.member_limit, p.customer_limit,
                       p.ai_credit_limit,
                       (SELECT COUNT(*) FROM users u WHERE u.tenant_id=t.id AND u.status='ACTIVE') members_used,
                       (SELECT COUNT(*) FROM customers c WHERE c.tenant_id=t.id) customers_used,
                       (SELECT COUNT(*) FROM ai_analysis a WHERE a.tenant_id=t.id) ai_used
                FROM tenants t JOIN plan_catalog p ON p.plan_code=t.plan_code WHERE t.id=?
                """, (rs, rowNum) -> new SubscriptionView(rs.getString("plan_code"), rs.getString("plan_name"),
                rs.getBigDecimal("monthly_price"), rs.getInt("members_used"), rs.getInt("member_limit"),
                rs.getInt("customers_used"), rs.getInt("customer_limit"), rs.getInt("ai_used"),
                rs.getInt("ai_credit_limit")), tenantId);
    }

    private MemberView member(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new MemberView(rs.getString("id"), rs.getString("tenant_id"), rs.getString("username"),
                rs.getString("display_name"), rs.getString("email"), valueOrEmpty(rs.getString("department_id")),
                rs.getString("department_name"), rs.getString("role_code"), rs.getString("data_scope"),
                rs.getString("status"), rs.getTimestamp("created_at").toInstant().toString());
    }

    private DepartmentView department(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new DepartmentView(rs.getString("id"), rs.getString("tenant_id"), rs.getString("name"),
                valueOrEmpty(rs.getString("parent_id")), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant().toString());
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private KnowledgeArticleView knowledge(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new KnowledgeArticleView(rs.getString("id"), rs.getString("tenant_id"), rs.getString("title"),
                rs.getString("category"), rs.getString("content"), rs.getBoolean("active_flag"),
                rs.getString("updated_by"), rs.getTimestamp("created_at").toInstant().toString(),
                rs.getTimestamp("updated_at").toInstant().toString());
    }
}
