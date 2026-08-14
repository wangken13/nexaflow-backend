package com.vebcoding.trade.tenant.mapper;

import com.vebcoding.trade.tenant.api.AuditLogView;
import com.vebcoding.trade.tenant.api.MemberView;
import com.vebcoding.trade.tenant.api.TenantProfileResponse;
import com.vebcoding.trade.tenant.api.ChannelConfigView;
import com.vebcoding.trade.tenant.api.KnowledgeArticleView;
import com.vebcoding.trade.tenant.api.SubscriptionView;
import com.vebcoding.trade.tenant.api.DepartmentView;
import com.vebcoding.trade.tenant.api.ImportJobView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTenantMapper implements TenantMapper {
    private final ConcurrentHashMap<String, MemberView> members = new ConcurrentHashMap<>();
    private final List<AuditLogView> audits = new ArrayList<>();
    private final ConcurrentHashMap<String, KnowledgeArticleView> knowledge = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ChannelConfigView> channels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DepartmentView> departments = new ConcurrentHashMap<>();

    public InMemoryTenantMapper() {
        members.put("demo-admin", new MemberView("demo-admin", "demo-tenant", "admin", "管理员", "",
                "OWNER", "ACTIVE", Instant.now().toString()));
    }

    @Override
    public TenantProfileResponse findProfile(String tenantId) {
        return new TenantProfileResponse(tenantId, "Demo 外贸团队", "PRO", 128, 3000);
    }

    @Override
    public List<MemberView> findMembers(String tenantId) {
        return members.values().stream().filter(member -> member.tenantId().equals(tenantId)).toList();
    }

    @Override
    public Optional<MemberView> findMember(String tenantId, String id) {
        return Optional.ofNullable(members.get(id)).filter(member -> member.tenantId().equals(tenantId));
    }

    @Override
    public MemberView saveMember(MemberView member, String passwordHash) {
        members.put(member.id(), member);
        return member;
    }

    @Override
    public MemberView updateMemberRole(String tenantId, String id, String role) {
        MemberView current = findMember(tenantId, id).orElseThrow();
        MemberView updated = new MemberView(current.id(), current.tenantId(), current.username(), current.displayName(),
                current.email(), current.departmentId(), current.departmentName(), role, current.dataScope(),
                current.status(), current.createdAt());
        members.put(id, updated);
        return updated;
    }

    @Override
    public MemberView updateMemberStatus(String tenantId, String id, String status) {
        MemberView current = findMember(tenantId, id).orElseThrow();
        MemberView updated = new MemberView(current.id(), current.tenantId(), current.username(), current.displayName(),
                current.email(), current.departmentId(), current.departmentName(), current.role(), current.dataScope(),
                status, current.createdAt());
        members.put(id, updated);
        return updated;
    }

    @Override
    public MemberView updateMemberAccess(String tenantId, String id, String departmentId, String dataScope) {
        MemberView current = findMember(tenantId, id).orElseThrow();
        String departmentName = findDepartment(tenantId, departmentId).map(DepartmentView::name).orElse("");
        MemberView updated = new MemberView(current.id(), current.tenantId(), current.username(), current.displayName(),
                current.email(), departmentId, departmentName, current.role(), dataScope, current.status(),
                current.createdAt());
        members.put(id, updated);
        return updated;
    }

    @Override public List<DepartmentView> findDepartments(String tenantId) {
        return departments.values().stream().filter(item -> tenantId.equals(item.tenantId())).toList();
    }
    @Override public Optional<DepartmentView> findDepartment(String tenantId, String id) {
        return Optional.ofNullable(departments.get(id)).filter(item -> tenantId.equals(item.tenantId()));
    }
    @Override public DepartmentView saveDepartment(DepartmentView department) {
        departments.put(department.id(), department); return department;
    }
    @Override public DepartmentView updateDepartmentStatus(String tenantId, String id, String status) {
        DepartmentView current = findDepartment(tenantId, id).orElseThrow();
        DepartmentView updated = new DepartmentView(current.id(), current.tenantId(), current.name(),
                current.parentId(), status, current.createdAt());
        departments.put(id, updated); return updated;
    }
    @Override public List<ImportJobView> findImportJobs(String tenantId) { return List.of(); }

    @Override
    public List<AuditLogView> findAuditLogs(String tenantId, String module, String keyword) {
        return audits.stream().filter(log -> log.tenantId().equals(tenantId)).toList().reversed();
    }

    @Override
    public AuditLogView saveAuditLog(AuditLogView auditLog) {
        audits.add(auditLog);
        return auditLog;
    }

    @Override public List<KnowledgeArticleView> findKnowledgeArticles(String tenantId) { return knowledge.values().stream().filter(item -> tenantId.equals(item.tenantId())).toList(); }
    @Override public Optional<KnowledgeArticleView> findKnowledgeArticle(String tenantId, String id) { return Optional.ofNullable(knowledge.get(id)).filter(item -> tenantId.equals(item.tenantId())); }
    @Override public KnowledgeArticleView saveKnowledgeArticle(KnowledgeArticleView article) { knowledge.put(article.id(), article); return article; }
    @Override public boolean deleteKnowledgeArticle(String tenantId, String id) { return findKnowledgeArticle(tenantId, id).map(item -> knowledge.remove(id) != null).orElse(false); }
    @Override public List<ChannelConfigView> findChannelConfigs(String tenantId) { return List.copyOf(channels.values()); }
    @Override public ChannelConfigView saveChannelConfig(String tenantId, ChannelConfigView channel, String updatedBy) { channels.put(channel.channelType(), channel); return channel; }
    @Override public SubscriptionView findSubscription(String tenantId) { return new SubscriptionView("PRO", "专业版", BigDecimal.valueOf(899), members.size(), 20, 0, 10000, 128, 3000); }
}
