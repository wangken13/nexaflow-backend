package com.vebcoding.trade.order.api;

public record OrderView(String id, String tenantId, String customerId, String customerName,
                        String productId, String productName, String status, String deliveryDate, boolean risk) {
}
