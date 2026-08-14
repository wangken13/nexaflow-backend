package com.vebcoding.trade.tenant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(@NotBlank @Size(max = 128) String name, @Size(max = 64) String parentId) {
}
