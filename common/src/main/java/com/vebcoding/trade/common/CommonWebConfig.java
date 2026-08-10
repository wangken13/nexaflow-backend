package com.vebcoding.trade.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebConfig {
    @Bean
    FilterRegistrationBean<TenantHeaderFilter> tenantHeaderFilter(
            @Value("${JWT_SECRET}") String jwtSecret, ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        FilterRegistrationBean<TenantHeaderFilter> registration = new FilterRegistrationBean<>();
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        registration.setFilter(new TenantHeaderFilter(jwtSecret, jdbcTemplate == null
                ? (sessionId, userId, tenantId, role) -> false : new JdbcSessionVerifier(jdbcTemplate)));
        registration.setOrder(-50);
        return registration;
    }

    @Bean
    FilterRegistrationBean<OperationAuditFilter> operationAuditFilter(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        FilterRegistrationBean<OperationAuditFilter> registration = new FilterRegistrationBean<>();
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            registration.setEnabled(false);
            registration.setFilter(new OperationAuditFilter(null));
        } else {
            registration.setFilter(new OperationAuditFilter(jdbcTemplate));
        }
        registration.setOrder(50);
        return registration;
    }
}
