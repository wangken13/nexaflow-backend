package com.vebcoding.trade.tenant.api;

import java.math.BigDecimal;

public record SubscriptionOrderView(String id, String planCode, String planName, int billingMonths,
                                    BigDecimal amount, String status, String checkoutUrl,
                                    String providerTransactionId, String createdAt, String paidAt,
                                    String expiresAt) {
}
