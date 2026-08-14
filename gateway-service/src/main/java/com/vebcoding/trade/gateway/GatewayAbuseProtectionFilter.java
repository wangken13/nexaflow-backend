package com.vebcoding.trade.gateway;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayAbuseProtectionFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(GatewayAbuseProtectionFilter.class);
    private static final Policy DEFAULT_POLICY = new Policy("api", 300, Duration.ofMinutes(1));
    private static final Map<String, Policy> AUTH_POLICIES = Map.of(
            "/api/auth/sms-codes", new Policy("sms", 8, Duration.ofHours(1)),
            "/api/auth/captcha", new Policy("captcha", 20, Duration.ofMinutes(10)),
            "/api/auth/login", new Policy("login", 20, Duration.ofMinutes(15)),
            "/api/auth/register", new Policy("register", 5, Duration.ofHours(1)));
    private static final Policy INBOUND_POLICY = new Policy("channel-inbound", 120, Duration.ofMinutes(1));
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final ReactiveStringRedisTemplate redisTemplate;

    public GatewayAbuseProtectionFilter(ObjectProvider<ReactiveStringRedisTemplate> redisTemplate) {
        this.redisTemplate = redisTemplate.getIfAvailable();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Policy policy = policy(exchange.getRequest().getURI().getPath());
        String key = policy.name() + ":" + clientIp(exchange.getRequest());
        return allowDistributed(key, policy)
                .flatMap(allowed -> allowed ? chain.filter(exchange) : reject(exchange));
    }

    @Override public int getOrder() { return -200; }

    private boolean allow(String key, Policy policy) {
        long now = System.nanoTime();
        Window window = windows.compute(key, (ignored, current) -> current == null || current.expiresAtNanos < now
                ? new Window(now + policy.window().toNanos()) : current);
        int count = window.count().incrementAndGet();
        if (windows.size() > 20_000) windows.entrySet().removeIf(entry -> entry.getValue().expiresAtNanos < now);
        return count <= policy.maxRequests();
    }

    private Mono<Boolean> allowDistributed(String key, Policy policy) {
        if (redisTemplate == null) return Mono.just(allow(key, policy));
        String redisKey = "nexaflow:rate:" + key;
        return redisTemplate.opsForValue().increment(redisKey)
                .flatMap(count -> count == 1
                        ? redisTemplate.expire(redisKey, policy.window()).thenReturn(true)
                        : Mono.just(count <= policy.maxRequests()))
                .onErrorResume(error -> {
                    log.warn("gateway.rate_limit.redis_unavailable fallback=local cause={}", error.getClass().getSimpleName());
                    return Mono.just(allow(key, policy));
                });
    }

    private Policy policy(String path) {
        if (path.startsWith("/api/inquiry/inbound/")) return INBOUND_POLICY;
        return AUTH_POLICIES.getOrDefault(path, DEFAULT_POLICY);
    }

    private String clientIp(ServerHttpRequest request) {
        return request.getRemoteAddress() == null || request.getRemoteAddress().getAddress() == null
                ? "unknown" : request.getRemoteAddress().getAddress().getHostAddress();
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        log.warn("gateway.rate_limit.rejected scope={}", policy(exchange.getRequest().getURI().getPath()).name());
        byte[] body = "{\"success\":false,\"code\":\"RATE_LIMITED\",\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}"
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().set("Retry-After", "60");
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private record Policy(String name, int maxRequests, Duration window) { }
    private record Window(long expiresAtNanos, AtomicInteger count) {
        private Window(long expiresAtNanos) { this(expiresAtNanos, new AtomicInteger()); }
    }
}
