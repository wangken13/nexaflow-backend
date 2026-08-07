package com.vebcoding.trade.quotation.api;

import java.math.BigDecimal;

public record QuotationView(String id, String tenantId, String customerId, String productName, int quantity,
                            BigDecimal unitPrice, String status, String createdAt) {
}