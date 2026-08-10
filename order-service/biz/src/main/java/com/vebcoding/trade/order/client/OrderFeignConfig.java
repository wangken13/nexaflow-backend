package com.vebcoding.trade.order.client;

import com.vebcoding.trade.common.TenantContext;
import feign.RequestInterceptor;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderFeignConfig {
    @Bean
    RequestInterceptor authenticatedRequestInterceptor() {
        return template -> template.header(
                HttpHeaders.AUTHORIZATION, "Bearer " + TenantContext.accessToken());
    }
}
