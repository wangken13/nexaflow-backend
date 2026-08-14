package com.vebcoding.trade.notification.api;

public record SupportMessageView(String id, String ticketId, String authorId, String content, String createdAt) {
}
