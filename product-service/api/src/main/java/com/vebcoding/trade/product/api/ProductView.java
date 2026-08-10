package com.vebcoding.trade.product.api;

import java.math.BigDecimal;

public record ProductView(String id, String tenantId, String sku, String name, String specification,
                          String currency, BigDecimal unitPrice, int moq, boolean active, String createdAt) {
}
