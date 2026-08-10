package com.vebcoding.trade.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsNotFoundBusinessError() {
        var response = handler.handleBusinessException(BusinessException.notFound("客户不存在"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("客户不存在");
    }

    @Test
    void mapsRateLimitBusinessError() {
        var response = handler.handleBusinessException(BusinessException.rateLimited("操作过于频繁"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().code()).isEqualTo("RATE_LIMITED");
    }

    @Test
    void mapsDatabaseConnectionFailureWithoutLeakingConnectionDetails() {
        var response = handler.handleDatabaseUnavailable();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().message()).isEqualTo("数据库暂时无法连接，请稍后重试");
        assertThat(response.getBody().message()).doesNotContain("secret-host");
    }

    @Test
    void mapsDataIntegrityConflict() {
        var response = handler.handleDataConflict();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("CONFLICT");
        assertThat(response.getBody().message()).doesNotContain("users.uk_phone");
    }

    @Test
    void unexpectedErrorReturnsTraceableReferenceWithoutLeakingDetails() {
        var response = handler.handleUnexpectedException(new IllegalStateException("database password is secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).contains("参考编号").doesNotContain("password");
    }
}
