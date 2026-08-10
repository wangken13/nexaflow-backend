package com.vebcoding.trade.common;

public final class TenantContext {
    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> USER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> ACCESS_TOKEN = new ThreadLocal<>();
    private static final ThreadLocal<String> SESSION = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        TENANT.set(tenantId);
    }

    public static String tenantId() {
        return require(TENANT.get(), "tenant");
    }

    public static void setUserId(String userId) {
        USER.set(userId);
    }

    public static String userId() {
        return require(USER.get(), "user");
    }

    public static void setRole(String role) {
        ROLE.set(role);
    }

    public static String role() {
        return require(ROLE.get(), "role");
    }

    public static void setAccessToken(String accessToken) {
        ACCESS_TOKEN.set(accessToken);
    }

    public static String accessToken() {
        return require(ACCESS_TOKEN.get(), "access token");
    }

    public static void setSessionId(String sessionId) { SESSION.set(sessionId); }

    public static String sessionId() { return require(SESSION.get(), "session"); }

    public static void clear() {
        TENANT.remove();
        USER.remove();
        ROLE.remove();
        ACCESS_TOKEN.remove();
        SESSION.remove();
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing authenticated " + field + " context");
        }
        return value;
    }
}
