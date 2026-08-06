package com.vebcoding.trade.gateway;

import com.vebcoding.trade.common.JwtSupport;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {
    private static final List<String> PUBLIC_PATHS = List.of("/api/auth/login", "/api/auth/register", "/actuator/health");

    @Value("${app.jwt.secret:change-me-in-production}")
    private String secret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            Map<String, String> claims = JwtSupport.verify(token, secret);
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.get("sub"))
                    .header("X-Tenant-Id", claims.get("tenantId"))
                    .header("X-Role", claims.get("role"))
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (Exception ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
