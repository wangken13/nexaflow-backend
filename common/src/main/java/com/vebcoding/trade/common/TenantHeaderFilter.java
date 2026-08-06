package com.vebcoding.trade.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class TenantHeaderFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            TenantContext.setTenantId(request.getHeader("X-Tenant-Id"));
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
