package com.vebcoding.trade.product.api;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record UpsertProductRequest(@NotBlank String sku, @NotBlank String name, String specification,
                                   String currency, BigDecimal unitPrice, int moq, boolean active) {
}
