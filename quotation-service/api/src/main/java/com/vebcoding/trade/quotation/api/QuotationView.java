package com.vebcoding.trade.quotation.api;

import java.math.BigDecimal;
import java.util.List;

public record QuotationView(String id, String tenantId, String customerId, String quotationNo, String productName,
                            int quantity, BigDecimal unitPrice, String currency, String tradeTerm,
                            String destinationPort, BigDecimal freight, BigDecimal totalAmount, String validUntil,
                            String notes, String status, List<QuotationItemView> items, String createdAt) {
    public QuotationView(String id, String tenantId, String customerId, String productName, int quantity,
                         BigDecimal unitPrice, String status, String createdAt) {
        this(id, tenantId, customerId, id, productName, quantity, unitPrice, "USD", "FOB", "",
                BigDecimal.ZERO, unitPrice.multiply(BigDecimal.valueOf(quantity)), "", "", status,
                List.of(new QuotationItemView("", "", productName, "", quantity, unitPrice,
                        unitPrice.multiply(BigDecimal.valueOf(quantity)))), createdAt);
    }
}
