package com.vebcoding.trade.tenant.mapper;

import com.vebcoding.trade.tenant.api.AuditLogView;
import com.vebcoding.trade.tenant.api.MemberView;
import com.vebcoding.trade.tenant.api.TenantProfileResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTenantMapper implements TenantMapper {
    private final ConcurrentHashMap<String, MemberView> members = new ConcurrentHashMap<>();
    private final List<AuditLogView> audits = new ArrayList<>();

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
                current.email(), role, current.status(), current.createdAt());
        members.put(id, updated);
        return updated;
    }

    @Override
    public MemberView updateMemberStatus(String tenantId, String id, String status) {
        MemberView current = findMember(tenantId, id).orElseThrow();
        MemberView updated = new MemberView(current.id(), current.tenantId(), current.username(), current.displayName(),
                current.email(), current.role(), status, current.createdAt());
        members.put(id, updated);
        return updated;
    }

    @Override
    public List<AuditLogView> findAuditLogs(String tenantId, String module, String keyword) {
        return audits.stream().filter(log -> log.tenantId().equals(tenantId)).toList().reversed();
    }

    @Override
    public AuditLogView saveAuditLog(AuditLogView auditLog) {
        audits.add(auditLog);
        return auditLog;
    }
}
