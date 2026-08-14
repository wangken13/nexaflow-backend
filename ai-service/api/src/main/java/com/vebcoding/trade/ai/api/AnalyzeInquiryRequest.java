package com.vebcoding.trade.ai.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalyzeInquiryRequest(String inquiryId, @NotBlank @Size(max = 10_000) String content) {
    public AnalyzeInquiryRequest(String content) {
        this("", content);
    }
}
