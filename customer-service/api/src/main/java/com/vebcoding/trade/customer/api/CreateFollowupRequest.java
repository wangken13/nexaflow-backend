package com.vebcoding.trade.customer.api;

import jakarta.validation.constraints.NotBlank;

public record CreateFollowupRequest(String type, @NotBlank String content, String operatorName) {
}
