package com.vebcoding.trade.inquiry.api;

import jakarta.validation.constraints.NotBlank;

public record CreateChannelCredentialRequest(@NotBlank String displayName, @NotBlank String channelType) {
}
