package com.vebcoding.trade.tenant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRefundRequest(@NotBlank @Size(max = 1000) String reason) {
}
