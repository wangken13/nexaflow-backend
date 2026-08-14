package com.vebcoding.trade.quotation.api;

public record QuotationApprovalView(String id, String quotationId, String action, String comment,
                                    String operatorId, String createdAt) {
}
