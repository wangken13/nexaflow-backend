package com.vebcoding.trade.tenant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberAccessRequest(@Size(max = 64) String departmentId, @NotBlank String dataScope) {
}
