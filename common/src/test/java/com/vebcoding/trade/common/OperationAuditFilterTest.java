package com.vebcoding.trade.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OperationAuditFilterTest {
    private final JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);

    @AfterEach
    void clear() { TenantContext.clear(); }

    @Test
    void recordsReadRequestWithAuthenticatedContext() throws Exception {
        TenantContext.setTenantId("tenant-a");
        TenantContext.setUserId("user-a");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/customer/cus-1");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");

        new OperationAuditFilter(jdbcTemplate).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    void skipsHealthAndCaptchaNoise() throws Exception {
        OperationAuditFilter filter = new OperationAuditFilter(jdbcTemplate);
        filter.doFilter(new MockHttpServletRequest("GET", "/actuator/health"),
                new MockHttpServletResponse(), new MockFilterChain());
        filter.doFilter(new MockHttpServletRequest("GET", "/auth/captcha"),
                new MockHttpServletResponse(), new MockFilterChain());

        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }
}
