package com.vebcoding.trade.quotation.api;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record UpsertApprovalRuleRequest(@NotBlank String name, @NotBlank String ruleType,
                                        BigDecimal thresholdAmount, String conditionValue,
                                        boolean enabled) {
}
