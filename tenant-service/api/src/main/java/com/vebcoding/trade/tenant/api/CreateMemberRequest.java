package com.vebcoding.trade.tenant.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMemberRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 128) String displayName,
        @Email @Size(max = 255) String email,
        @NotBlank String role) {
}
