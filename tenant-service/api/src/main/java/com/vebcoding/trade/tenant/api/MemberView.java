package com.vebcoding.trade.tenant.api;

public record MemberView(String id, String tenantId, String username, String displayName, String email,
                         String role, String status, String createdAt) {
}
