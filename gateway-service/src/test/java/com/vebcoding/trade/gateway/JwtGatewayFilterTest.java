package com.vebcoding.trade.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.common.JwtSupport;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;

class JwtGatewayFilterTest {
    private static final String SECRET = "test-secret-with-at-least-32-bytes-long";

    @Test
    void weakJwtSecretIsRejectedDuringGatewayStartup() {
        JwtGatewayFilter filter = new JwtGatewayFilter();
        ReflectionTestUtils.setField(filter, "secret", "weak-secret");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, filter::validateConfiguration);
    }

    @Test
    void trustedTokenIsForwardedWithoutIdentityHeaders() {
        String token = JwtSupport.create("user-1", "tenant-1", "SALES", SECRET, 300);
        JwtGatewayFilter filter = filter();
        AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/customer/customers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-User-Id", "spoofed-user")
                .header("X-Tenant-Id", "spoofed-tenant")
                .build());

        filter.filter(exchange, current -> {
            forwarded.set(current.getRequest());
            return current.getResponse().setComplete();
        }).block();

        assertThat(forwarded.get().getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);
        assertThat(forwarded.get().getHeaders().getFirst("X-User-Id")).isNull();
        assertThat(forwarded.get().getHeaders().getFirst("X-Tenant-Id")).isNull();
        assertThat(forwarded.get().getHeaders().getFirst("X-Role")).isNull();
        assertThat(forwarded.get().getHeaders().getFirst("X-Internal-Token")).isNull();
    }

    @Test
    void publicCaptchaPathStripsSpoofedIdentityHeadersWithoutRequiringToken() {
        JwtGatewayFilter filter = filter();
        AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/captcha")
                .header("X-User-Id", "spoofed-user")
                .header("X-Tenant-Id", "spoofed-tenant")
                .build());

        filter.filter(exchange, current -> {
            forwarded.set(current.getRequest());
            return current.getResponse().setComplete();
        }).block();

        assertThat(forwarded.get().getHeaders().getFirst("X-User-Id")).isNull();
        assertThat(forwarded.get().getHeaders().getFirst("X-Tenant-Id")).isNull();
    }

    @Test
    void readinessPathDoesNotRequireAuthentication() {
        JwtGatewayFilter filter = filter();
        AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/readyz").build());

        filter.filter(exchange, current -> {
            forwarded.set(current.getRequest());
            return current.getResponse().setComplete();
        }).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void exactHealthPathDoesNotRequireAuthentication() {
        JwtGatewayFilter filter = filter();
        AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build());

        filter.filter(exchange, current -> {
            forwarded.set(current.getRequest());
            return current.getResponse().setComplete();
        }).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void inboundChannelPathDoesNotAcceptSpoofedUserIdentity() {
        JwtGatewayFilter filter = filter();
        AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/inquiry/inbound/key-1")
                .header("X-User-Id", "spoofed-user")
                .header("X-Tenant-Id", "spoofed-tenant")
                .build());

        filter.filter(exchange, current -> {
            forwarded.set(current.getRequest());
            return current.getResponse().setComplete();
        }).block();

        assertThat(forwarded.get().getHeaders().getFirst("X-User-Id")).isNull();
        assertThat(forwarded.get().getHeaders().getFirst("X-Tenant-Id")).isNull();
    }

    @Test
    void invalidTokenIsRejectedBeforeRouteForwarding() {
        JwtGatewayFilter filter = filter();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/customer/customers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build());

        filter.filter(exchange, current -> current.getResponse().setComplete()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void pathThatOnlyStartsWithPublicLoginPathStillRequiresAuthentication() {
        JwtGatewayFilter filter = filter();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/auth/login-admin").build());

        filter.filter(exchange, current -> current.getResponse().setComplete()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private JwtGatewayFilter filter() {
        JwtGatewayFilter filter = new JwtGatewayFilter();
        ReflectionTestUtils.setField(filter, "secret", SECRET);
        return filter;
    }
}
