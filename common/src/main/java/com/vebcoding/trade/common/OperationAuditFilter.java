package com.vebcoding.trade.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

public class OperationAuditFilter extends OncePerRequestFilter {
    private static final Log LOGGER = LogFactory.getLog(OperationAuditFilter.class);
    private final JdbcTemplate jdbcTemplate;

    public OperationAuditFilter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getMethod().equals("OPTIONS")
                || request.getRequestURI().equals("/auth/captcha")
                || request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            persist(request, response, startedAt);
        }
    }

    private void persist(HttpServletRequest request, HttpServletResponse response, long startedAt) {
        try {
            String tenantId = safeContext(TenantContext::tenantId, "public");
            String userId = safeContext(TenantContext::userId, "anonymous");
            String requestId = request.getHeader("X-Request-Id");
            if (requestId == null || requestId.isBlank()) requestId = UUID.randomUUID().toString();
            jdbcTemplate.update("""
                    INSERT INTO operation_audit_logs
                      (id, tenant_id, user_id, request_id, http_method, request_path, client_ip, user_agent,
                       response_status, duration_ms, occurred_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, "op-" + UUID.randomUUID(), tenantId, userId, limit(requestId, 64), request.getMethod(),
                    limit(request.getRequestURI(), 255), limit(clientIp(request), 64),
                    limit(valueOrEmpty(request.getHeader("User-Agent")), 255), response.getStatus(),
                    Math.max(0, System.currentTimeMillis() - startedAt), java.sql.Timestamp.from(Instant.now()));
        } catch (Exception exception) {
            LOGGER.error("Failed to persist operation audit for " + request.getMethod() + " "
                    + request.getRequestURI(), exception);
        }
    }

    private String safeContext(ContextSupplier supplier, String fallback) {
        try {
            return supplier.get();
        } catch (IllegalStateException exception) {
            return fallback;
        }
    }

    private String limit(String value, int length) {
        return value.length() <= length ? value : value.substring(0, length);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",", 2)[0].trim();
        return valueOrEmpty(request.getRemoteAddr());
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    @FunctionalInterface
    private interface ContextSupplier {
        String get();
    }
}
