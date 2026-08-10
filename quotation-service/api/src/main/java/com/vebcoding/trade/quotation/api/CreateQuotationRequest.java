package com.vebcoding.trade.quotation.api;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

public record CreateQuotationRequest(@NotBlank String customerId, String currency, String tradeTerm,
                                     String destinationPort, BigDecimal freight, String validUntil, String notes,
                                     List<QuotationItemRequest> items) {
    public CreateQuotationRequest(String customerId, String productName, int quantity, BigDecimal unitPrice) {
        this(customerId, "USD", "FOB", "", BigDecimal.ZERO, "", "",
                List.of(new QuotationItemRequest("", productName, "", quantity, unitPrice)));
    }
}
