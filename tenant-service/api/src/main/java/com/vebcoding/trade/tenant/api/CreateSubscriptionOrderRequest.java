package com.vebcoding.trade.tenant.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateSubscriptionOrderRequest(@NotBlank String planCode, @Min(1) @Max(36) int billingMonths) {
}
