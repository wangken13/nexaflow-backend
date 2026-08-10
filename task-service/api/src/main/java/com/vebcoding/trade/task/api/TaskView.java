package com.vebcoding.trade.task.api;

public record TaskView(String id, String tenantId, String title, String priority, String status, String dueAt,
                       String relatedType, String relatedId) {
    public TaskView(String id, String tenantId, String title, String priority, String status, String dueAt) {
        this(id, tenantId, title, priority, status, dueAt, "", "");
    }
}
