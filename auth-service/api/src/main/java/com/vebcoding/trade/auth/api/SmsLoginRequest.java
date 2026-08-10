package com.vebcoding.trade.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SmsLoginRequest(
        @NotBlank @Size(max = 32) String phone,
        @NotBlank @Size(min = 6, max = 6) String verificationCode) {
}
