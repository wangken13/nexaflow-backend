package com.vebcoding.trade.inquiry.api;

import jakarta.validation.constraints.NotBlank;

public record CreateInquiryRequest(@NotBlank String customerId, @NotBlank String subject, @NotBlank String content) {
}