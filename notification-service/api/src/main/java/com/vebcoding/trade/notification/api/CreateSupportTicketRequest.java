package com.vebcoding.trade.notification.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupportTicketRequest(@NotBlank String category, @NotBlank String priority,
                                         @NotBlank @Size(max = 255) String subject,
                                         @NotBlank @Size(max = 4000) String description) {
}
