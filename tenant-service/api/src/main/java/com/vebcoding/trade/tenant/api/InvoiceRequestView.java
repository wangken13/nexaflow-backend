package com.vebcoding.trade.tenant.api;

public record InvoiceRequestView(String id, String orderId, String invoiceTitle, String taxNumber,
                                 String recipientEmail, String status, String createdAt) {
}
