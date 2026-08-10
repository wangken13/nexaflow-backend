package com.vebcoding.trade.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 128) String tenantName,
        @NotBlank @Size(min = 4, max = 64) String username,
        @NotBlank @Size(min = 10, max = 128) String password,
        @NotBlank @Size(max = 128) String displayName,
        @NotBlank @Size(max = 32) String phone,
        @NotBlank @Size(min = 6, max = 6) String verificationCode) {
}
