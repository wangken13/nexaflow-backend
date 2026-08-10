package com.vebcoding.trade.tenant.api;

public record AuditLogView(String id, String tenantId, String actor, String module, String action,
                           String targetId, String detail, String createdAt) {
}
