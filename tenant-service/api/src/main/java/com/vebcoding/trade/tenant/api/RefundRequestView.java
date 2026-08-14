package com.vebcoding.trade.tenant.api;

public record RefundRequestView(String id, String orderId, String reason, String status, String createdAt) {
}
