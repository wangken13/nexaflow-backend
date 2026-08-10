package com.vebcoding.trade.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityResponseHeadersFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            var headers = exchange.getResponse().getHeaders();
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("X-Frame-Options", "DENY");
            headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
            headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
            if (exchange.getRequest().getURI().getPath().startsWith("/api/auth/")) {
                headers.setCacheControl("no-store, max-age=0");
                headers.setPragma("no-cache");
            }
            return Mono.empty();
        });
        return chain.filter(exchange);
    }

    @Override public int getOrder() { return -190; }
}
