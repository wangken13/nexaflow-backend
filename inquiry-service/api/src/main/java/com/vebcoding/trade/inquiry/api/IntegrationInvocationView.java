package com.vebcoding.trade.inquiry.api;

public record IntegrationInvocationView(String id, String credentialId, String requestId, String httpMethod,
                                        String requestPath, String clientIp, String outcome, long durationMs,
                                        String createdAt) {
}
