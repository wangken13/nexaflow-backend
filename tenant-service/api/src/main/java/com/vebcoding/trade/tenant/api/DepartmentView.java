package com.vebcoding.trade.tenant.api;

public record DepartmentView(String id, String tenantId, String name, String parentId, String status,
                             String createdAt) {
}
