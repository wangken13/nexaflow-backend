package com.vebcoding.trade.inquiry.api;

public record InboundInquiryRequest(String externalId, String customerName, String contactName, String email,
                                    String phone, String country, String subject, String content) {
}
