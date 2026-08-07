package com.vebcoding.trade.auth.api;

public record CurrentUserResponse(String username, String tenantId, String role) {
}