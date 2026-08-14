package com.vebcoding.trade.tenant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertChannelConfigRequest(@NotBlank @Size(max = 32) String channelType,
                                         @NotBlank @Size(max = 100) String displayName,
                                         @Size(max = 255) String accountRef,
                                         boolean enabled) {
}
