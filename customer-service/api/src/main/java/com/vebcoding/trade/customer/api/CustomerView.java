package com.vebcoding.trade.customer.api;

public record CustomerView(String id, String tenantId, String name, String country, String tag, String createdAt) {
}