package com.vebcoding.trade.gateway;

import com.vebcoding.trade.common.JwtSupport;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/captcha", "/api/auth/login", "/api/auth/refresh", "/api/auth/register", "/api/auth/sms-codes", "/api/auth/sms-login",
            "/api/auth/wechat/", "/api/inquiry/inbound/", "/api/tenant/billing/callback", "/actuator/health");

    @Value("${JWT_SECRET}")
    private String secret;

    @PostConstruct
    void validateConfiguration() {
        JwtSupport.validateSecret(secret);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(withoutIdentityHeaders(exchange));
        }
        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (token == null || token.isBlank()) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录后再执行此操作");
        }
        if (!token.startsWith("Bearer ") || token.length() <= 7) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "登录凭证格式错误，请重新登录");
        }
        token = token.substring(7);
        try {
            JwtSupport.verify(token, secret);
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-Tenant-Id");
                        headers.remove("X-Role");
                        headers.remove("X-Internal-Token");
                    })
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (ExpiredJwtException ex) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "登录状态已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException ex) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "登录凭证无效，请重新登录");
        } catch (IllegalStateException ex) {
            return error(exchange, HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "认证服务配置异常，请联系管理员");
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private ServerWebExchange withoutIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove("X-User-Id");
            headers.remove("X-Tenant-Id");
            headers.remove("X-Role");
            headers.remove("X-Internal-Token");
        }).build();
        return exchange.mutate().request(request).build();
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        byte[] body = ("{\"success\":false,\"code\":\"" + code + "\",\"message\":\"" + message
                + "\",\"data\":null}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
