package com.vebcoding.trade.inquiry.service;

record InboundReceipt(String tenantId, String credentialId, String externalId, String payloadHash,
                      String customerId, String inquiryId, String status) {
}
