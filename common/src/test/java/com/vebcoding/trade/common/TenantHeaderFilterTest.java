package com.vebcoding.trade.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantHeaderFilterTest {
    private static final String SECRET = "test-secret-with-at-least-32-bytes-long";

    @Test
    void weakJwtSecretIsRejectedAtStartup() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, () -> new TenantHeaderFilter("weak-secret"));
    }

    @Test
    void captchaEndpointDoesNotRequireGatewayIdentityHeaders() throws Exception {
        TenantHeaderFilter filter = new TenantHeaderFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/captcha");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.doFilter(request, response, (currentRequest, currentResponse) -> forwarded.set(true));

        assertThat(forwarded).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void spoofedIdentityHeadersWithoutJwtAreRejected() throws Exception {
        TenantHeaderFilter filter = new TenantHeaderFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/customer/customers");
        request.addHeader("X-Tenant-Id", "spoofed-tenant");
        request.addHeader("X-User-Id", "spoofed-user");
        request.addHeader("X-Role", "OWNER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.doFilter(request, response, (currentRequest, currentResponse) -> forwarded.set(true));

        assertThat(forwarded).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void validJwtEstablishesIdentityAndIgnoresSpoofedHeaders() throws Exception {
        TenantHeaderFilter filter = new TenantHeaderFilter(SECRET);
        String token = JwtSupport.create("user-1", "tenant-1", "SALES", SECRET, 300);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/customer/customers");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        request.addHeader("X-Tenant-Id", "spoofed-tenant");
        request.addHeader("X-Role", "OWNER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean verified = new AtomicBoolean();

        filter.doFilter(request, response, (currentRequest, currentResponse) -> {
            assertThat(TenantContext.tenantId()).isEqualTo("tenant-1");
            assertThat(TenantContext.userId()).isEqualTo("user-1");
            assertThat(TenantContext.role()).isEqualTo("SALES");
            assertThat(TenantContext.accessToken()).isEqualTo(token);
            verified.set(true);
        });

        assertThat(verified).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, TenantContext::tenantId);
    }
}
