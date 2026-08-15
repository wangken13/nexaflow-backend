package com.vebcoding.trade.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class GatewayAbuseProtectionFilterTest {
    @Test
    void healthEndpointsBypassDistributedRateLimiting() {
        var beans = new StaticListableBeanFactory();
        var filter = new GatewayAbuseProtectionFilter(beans.getBeanProvider(ReactiveStringRedisTemplate.class));
        var accepted = new AtomicInteger();

        for (int index = 0; index < 400; index++) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/readyz").build());
            filter.filter(exchange, ignored -> {
                accepted.incrementAndGet();
                return reactor.core.publisher.Mono.empty();
            }).block();
        }

        assertThat(accepted.get()).isEqualTo(400);
    }

    @Test
    void fallsBackToLocalLimitAndRejectsCaptchaFlood() {
        var beans = new StaticListableBeanFactory();
        var filter = new GatewayAbuseProtectionFilter(beans.getBeanProvider(ReactiveStringRedisTemplate.class));
        var accepted = new AtomicInteger();

        for (int index = 0; index < 21; index++) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/captcha").build());
            filter.filter(exchange, ignored -> {
                accepted.incrementAndGet();
                return reactor.core.publisher.Mono.empty();
            }).block();
            if (index == 20) assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        assertThat(accepted.get()).isEqualTo(20);
    }
}
