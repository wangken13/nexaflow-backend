package com.vebcoding.trade.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

public class OperationAuditFilter extends OncePerRequestFilter {
    private static final Log LOGGER = LogFactory.getLog(OperationAuditFilter.class);
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final JdbcTemplate jdbcTemplate;

    public OperationAuditFilter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !WRITE_METHODS.contains(request.getMethod())
                || request.getRequestURI().equals("/auth/login")
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
            jdbcTemplate.update("""
                    INSERT INTO operation_audit_logs
                      (id, tenant_id, user_id, http_method, request_path, response_status, duration_ms, occurred_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, "op-" + UUID.randomUUID(), tenantId, userId, request.getMethod(),
                    limit(request.getRequestURI(), 255), response.getStatus(),
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

    @FunctionalInterface
    private interface ContextSupplier {
        String get();
    }
}
