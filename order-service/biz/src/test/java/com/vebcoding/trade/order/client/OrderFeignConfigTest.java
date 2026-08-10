package com.vebcoding.trade.order.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.common.TenantContext;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class OrderFeignConfigTest {
    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void forwardsVerifiedBearerTokenInsteadOfIdentityHeaders() {
        TenantContext.setAccessToken("verified-access-token");
        RequestTemplate template = new RequestTemplate();

        new OrderFeignConfig().authenticatedRequestInterceptor().apply(template);

        assertThat(template.headers().get(HttpHeaders.AUTHORIZATION))
                .containsExactly("Bearer verified-access-token");
        assertThat(template.headers()).doesNotContainKeys(
                "X-Tenant-Id", "X-User-Id", "X-Role", "X-Internal-Token");
    }
}
