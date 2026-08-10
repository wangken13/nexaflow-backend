package com.vebcoding.trade.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtSupportTest {
    private static final String SECRET = "test-secret-with-at-least-32-bytes-long";

    @Test
    void createsAndVerifiesTrustedIdentityClaims() {
        String token = JwtSupport.create("user-1", "tenant-1", "ADMIN", SECRET, 300);

        assertThat(JwtSupport.verify(token, SECRET))
                .containsEntry("sub", "user-1")
                .containsEntry("tenantId", "tenant-1")
                .containsEntry("role", "ADMIN");
    }

    @Test
    void rejectsWeakSigningSecret() {
        assertThatThrownBy(() -> JwtSupport.create("user-1", "tenant-1", "ADMIN", "too-short", 300))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
