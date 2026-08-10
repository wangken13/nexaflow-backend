package com.vebcoding.trade.quotation.api;

import java.math.BigDecimal;

public record QuotationItemRequest(String productId, String productName, String specification, int quantity,
                                   BigDecimal unitPrice) {
}
