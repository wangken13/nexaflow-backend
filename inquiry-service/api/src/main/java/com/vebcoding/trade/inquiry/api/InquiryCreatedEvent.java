package com.vebcoding.trade.inquiry.api;

public record InquiryCreatedEvent(String inquiryId, String tenantId, String content) {
}