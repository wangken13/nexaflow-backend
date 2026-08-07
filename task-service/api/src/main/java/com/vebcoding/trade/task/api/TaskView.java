package com.vebcoding.trade.task.api;

public record TaskView(String id, String tenantId, String title, String priority, String status, String dueAt) {
}