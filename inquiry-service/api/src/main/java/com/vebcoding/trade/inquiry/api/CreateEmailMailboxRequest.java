package com.vebcoding.trade.inquiry.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateEmailMailboxRequest(@NotBlank String displayName, @Email @NotBlank String emailAddress,
                                        @NotBlank String host, @Min(1) @Max(65535) int port,
                                        @NotBlank String username, @NotBlank String password, String folder) {
}
