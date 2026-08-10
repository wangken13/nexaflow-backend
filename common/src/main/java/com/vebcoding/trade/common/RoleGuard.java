package com.vebcoding.trade.common;

import java.util.Arrays;

public final class RoleGuard {
    private RoleGuard() {
    }

    public static void requireAny(String... roles) {
        String currentRole = TenantContext.role();
        boolean permitted = Arrays.stream(roles).anyMatch(role -> role.equalsIgnoreCase(currentRole));
        if (!permitted) {
            throw new AccessDeniedException("当前账号没有执行此操作的权限");
        }
    }
}
