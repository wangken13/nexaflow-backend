package com.vebcoding.trade.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

public class TenantHeaderFilter extends OncePerRequestFilter {
    private static final Set<String> ROLES = Set.of("OWNER", "ADMIN", "SALES", "OPERATOR", "VIEWER");
    private final String jwtSecret;
    private final SessionVerifier sessionVerifier;

    public TenantHeaderFilter(String jwtSecret) {
        this(jwtSecret, (sessionId, userId, tenantId, role) -> true);
    }

    public TenantHeaderFilter(String jwtSecret, SessionVerifier sessionVerifier) {
        JwtSupport.validateSecret(jwtSecret);
        this.jwtSecret = jwtSecret;
        this.sessionVerifier = sessionVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            unauthorized(response, "请先登录后再执行此操作");
            return;
        }
        String accessToken = authorization.substring(7).trim();
        try {
            Map<String, String> claims = JwtSupport.verify(accessToken, jwtSecret);
            String tenantId = claims.get("tenantId");
            String userId = claims.get("sub");
            String role = claims.get("role");
            String sessionId = claims.get("sid");
            if (!"ACCESS".equals(claims.get("typ")) || !validIdentity(tenantId, userId, role)
                    || !validHeader(sessionId) || !sessionVerifier.isActive(sessionId, userId, tenantId, role)) {
                unauthorized(response, "登录凭证缺少有效的用户身份，请重新登录");
                return;
            }
            TenantContext.setTenantId(tenantId);
            TenantContext.setUserId(userId);
            TenantContext.setRole(role.toUpperCase());
            TenantContext.setAccessToken(accessToken);
            TenantContext.setSessionId(sessionId);
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException exception) {
            unauthorized(response, "登录状态已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException exception) {
            unauthorized(response, "登录凭证无效，请重新登录");
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isPublicPath(String path) {
        return path.equals("/auth/captcha") || path.equals("/auth/login") || path.equals("/auth/refresh") || path.equals("/auth/register") || path.equals("/auth/sms-codes")
                || path.equals("/auth/sms-login") || path.startsWith("/auth/wechat/") || path.startsWith("/actuator/health");
    }

    private boolean validIdentity(String tenantId, String userId, String role) {
        return validHeader(tenantId) && validHeader(userId) && role != null && ROLES.contains(role.toUpperCase());
    }

    private boolean validHeader(String value) {
        return value != null && !value.isBlank() && value.length() <= 64;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\""
                + message + "\",\"data\":null}");
    }
}
