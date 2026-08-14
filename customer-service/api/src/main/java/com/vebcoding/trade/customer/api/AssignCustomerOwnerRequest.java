package com.vebcoding.trade.customer.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignCustomerOwnerRequest(@NotBlank @Size(max = 64) String ownerId) {
}
