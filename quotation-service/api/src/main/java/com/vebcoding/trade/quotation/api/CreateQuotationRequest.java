package com.vebcoding.trade.quotation.api;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CreateQuotationRequest(@NotBlank String customerId, @NotBlank String productName, int quantity,
                                     BigDecimal unitPrice) {
}