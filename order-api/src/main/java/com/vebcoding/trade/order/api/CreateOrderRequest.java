package com.vebcoding.trade.order.api;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(@NotBlank String customerName, @NotBlank String productName,
                                 @NotBlank String deliveryDate) {
}