package com.vebcoding.trade.inquiry.api;

import jakarta.validation.constraints.NotBlank;

public record CreateInquiryRequest(
        @NotBlank String customerId,
        @NotBlank String subject,
        @NotBlank String content,
        String analysisMode) {

    public CreateInquiryRequest(String customerId, String subject, String content) {
        this(customerId, subject, content, "ASYNC");
    }

    public boolean streamAnalysisRequested() {
        return "STREAM".equalsIgnoreCase(analysisMode);
    }
}
