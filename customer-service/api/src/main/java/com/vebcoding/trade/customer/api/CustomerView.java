package com.vebcoding.trade.customer.api;

public record CustomerView(String id, String tenantId, String name, String country, String tag,
                           String ownerId, String ownerName, String departmentId, String departmentName,
                           String createdAt) {
    public CustomerView(String id, String tenantId, String name, String country, String tag, String createdAt) {
        this(id, tenantId, name, country, tag, "", "", "", "", createdAt);
    }
}
