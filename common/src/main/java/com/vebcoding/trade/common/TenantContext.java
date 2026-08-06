package com.vebcoding.trade.common;

public final class TenantContext {
    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        TENANT.set(tenantId);
    }

    public static String tenantId() {
        return TENANT.get() == null ? "demo-tenant" : TENANT.get();
    }

    public static void clear() {
        TENANT.remove();
    }
}
