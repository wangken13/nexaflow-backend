package com.vebcoding.trade.customer.api;

public record ContactView(String id, String customerId, String name, String email, String phone, String position,
                          boolean primary, String createdAt) {
}
