package com.vebcoding.trade.tenant.api;

public record TenantProfileResponse(String tenantId, String name, String plan, int aiCreditsUsed, int aiCreditsLimit) {
}