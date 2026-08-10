package com.vebcoding.trade.common;

public interface SessionVerifier {
    boolean isActive(String sessionId, String userId, String tenantId, String role);
}
