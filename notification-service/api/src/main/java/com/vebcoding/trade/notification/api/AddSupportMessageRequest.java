package com.vebcoding.trade.notification.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddSupportMessageRequest(@NotBlank @Size(max = 4000) String content) {
}
