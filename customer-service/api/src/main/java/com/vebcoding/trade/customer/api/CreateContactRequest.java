package com.vebcoding.trade.customer.api;

import jakarta.validation.constraints.NotBlank;

public record CreateContactRequest(@NotBlank String name, String email, String phone, String position,
                                   boolean primary) {
}
