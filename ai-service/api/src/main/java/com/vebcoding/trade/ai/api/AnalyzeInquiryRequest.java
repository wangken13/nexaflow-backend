package com.vebcoding.trade.ai.api;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeInquiryRequest(String inquiryId, @NotBlank String content) {
    public AnalyzeInquiryRequest(String content) {
        this("", content);
    }
}
