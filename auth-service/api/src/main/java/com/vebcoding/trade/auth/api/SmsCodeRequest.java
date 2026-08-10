package com.vebcoding.trade.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SmsCodeRequest(
        @NotBlank @Size(max = 32) String phone,
        @NotBlank String purpose) {
}
