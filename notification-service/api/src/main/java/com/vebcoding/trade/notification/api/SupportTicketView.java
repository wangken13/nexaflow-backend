package com.vebcoding.trade.notification.api;

import java.util.List;

public record SupportTicketView(String id, String tenantId, String createdBy, String category, String priority,
                                String subject, String description, String status, String assignedTo,
                                String createdAt, String updatedAt, List<SupportMessageView> messages) {
}
