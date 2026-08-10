package com.vebcoding.trade.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {
    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void rejectsRequestsWithoutAuthenticatedIdentity() {
        assertThatThrownBy(TenantContext::tenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenant");
    }

    @Test
    void exposesCompleteAuthenticatedIdentity() {
        TenantContext.setTenantId("tenant-1");
        TenantContext.setUserId("user-1");
        TenantContext.setRole("ADMIN");

        assertThat(TenantContext.tenantId()).isEqualTo("tenant-1");
        assertThat(TenantContext.userId()).isEqualTo("user-1");
        assertThat(TenantContext.role()).isEqualTo("ADMIN");
    }
}
