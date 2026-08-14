package com.vebcoding.trade.tenant.api;

import java.math.BigDecimal;

public record SubscriptionView(String planCode, String planName, BigDecimal monthlyPrice,
                               int membersUsed, int memberLimit, int customersUsed, int customerLimit,
                               int aiCreditsUsed, int aiCreditLimit) {
}
