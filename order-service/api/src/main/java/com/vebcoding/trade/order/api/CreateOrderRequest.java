package com.vebcoding.trade.order.api;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(@NotBlank String customerId, @NotBlank String productId,
                                 @NotBlank String deliveryDate) {
}
