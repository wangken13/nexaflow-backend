package com.vebcoding.trade.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebConfig {
    @Bean
    FilterRegistrationBean<TenantHeaderFilter> tenantHeaderFilter() {
        FilterRegistrationBean<TenantHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantHeaderFilter());
        registration.setOrder(-50);
        return registration;
    }
}
