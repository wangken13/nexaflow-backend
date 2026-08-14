package com.vebcoding.trade.tenant.api;

public record KnowledgeArticleView(String id, String tenantId, String title, String category, String content,
                                   boolean active, String updatedBy, String createdAt, String updatedAt) {
}
