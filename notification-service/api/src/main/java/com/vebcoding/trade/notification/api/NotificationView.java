package com.vebcoding.trade.notification.api;

public record NotificationView(String id, String tenantId, String title, String content, boolean read, String createdAt) {
}