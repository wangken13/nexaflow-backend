package com.vebcoding.trade.quotation.api;

import java.math.BigDecimal;

public record ApprovalRuleView(String id, String tenantId, String name, String ruleType,
                               BigDecimal thresholdAmount, String conditionValue, boolean enabled,
                               String createdAt, String updatedAt) {
}
