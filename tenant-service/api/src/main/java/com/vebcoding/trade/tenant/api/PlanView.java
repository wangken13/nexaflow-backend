package com.vebcoding.trade.tenant.api;

import java.math.BigDecimal;

public record PlanView(String planCode, String planName, int memberLimit, int customerLimit, int aiCreditLimit,
                       BigDecimal monthlyPrice) {
}
