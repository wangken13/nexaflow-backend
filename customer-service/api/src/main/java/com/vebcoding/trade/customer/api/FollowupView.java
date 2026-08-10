package com.vebcoding.trade.customer.api;

public record FollowupView(String id, String customerId, String type, String content, String operatorName,
                           String createdAt) {
}
