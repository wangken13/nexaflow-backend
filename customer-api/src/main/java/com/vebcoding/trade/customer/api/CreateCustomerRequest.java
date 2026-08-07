package com.vebcoding.trade.customer.api;

import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(@NotBlank String name, String country, String tag) {
}