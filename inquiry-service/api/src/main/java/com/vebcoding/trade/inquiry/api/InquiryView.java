package com.vebcoding.trade.inquiry.api;

public record InquiryView(String id, String tenantId, String customerId, String subject, String content, String status,
                          String sourceChannel, String externalId, String ownerId, String nextActionDue,
                          String createdAt) {
    public InquiryView(String id, String tenantId, String customerId, String subject, String content, String status,
                       String createdAt) {
        this(id, tenantId, customerId, subject, content, status, "MANUAL", "", "", "", createdAt);
    }
}
