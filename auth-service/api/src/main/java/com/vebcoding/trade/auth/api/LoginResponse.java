package com.vebcoding.trade.auth.api;

public record LoginResponse(String token, String userId, String tenantId, String role, String username, String sessionId,
                            int expiresInSeconds) {
}
