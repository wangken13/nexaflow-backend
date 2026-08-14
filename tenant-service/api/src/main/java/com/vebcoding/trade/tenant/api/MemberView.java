package com.vebcoding.trade.tenant.api;

public record MemberView(String id, String tenantId, String username, String displayName, String email,
                         String departmentId, String departmentName, String role, String dataScope,
                         String status, String createdAt) {
    public MemberView(String id, String tenantId, String username, String displayName, String email,
                      String role, String status, String createdAt) {
        this(id, tenantId, username, displayName, email, "", "", role,
                defaultScope(role), status, createdAt);
    }

    private static String defaultScope(String role) {
        if ("OWNER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) return "ALL";
        if ("OPERATOR".equalsIgnoreCase(role)) return "DEPARTMENT";
        return "SELF";
    }
}
