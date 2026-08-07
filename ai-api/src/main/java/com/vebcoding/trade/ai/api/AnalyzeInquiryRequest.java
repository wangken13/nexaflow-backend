package com.vebcoding.trade.ai.api;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeInquiryRequest(@NotBlank String content) {
}