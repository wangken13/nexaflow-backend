package com.vebcoding.trade.inquiry.api;

public record InboundInquiryResponse(String inquiryId, String customerId, boolean duplicate, String status) {
}
