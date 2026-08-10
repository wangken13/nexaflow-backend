package com.vebcoding.trade.quotation.api;

import java.math.BigDecimal;

public record QuotationItemView(String id, String productId, String productName, String specification, int quantity,
                                BigDecimal unitPrice, BigDecimal amount) {
}
