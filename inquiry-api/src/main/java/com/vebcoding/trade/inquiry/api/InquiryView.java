package com.vebcoding.trade.inquiry.api;

public record InquiryView(String id, String tenantId, String customerId, String subject, String content, String status,
                          String createdAt) {
}