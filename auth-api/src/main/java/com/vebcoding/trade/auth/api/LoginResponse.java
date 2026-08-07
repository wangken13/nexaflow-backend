package com.vebcoding.trade.auth.api;

public record LoginResponse(String token, String tenantId, String role) {
}