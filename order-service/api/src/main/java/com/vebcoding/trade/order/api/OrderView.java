package com.vebcoding.trade.order.api;

public record OrderView(String id, String tenantId, String customerName, String productName, String status,
                        String deliveryDate, boolean risk) {
}