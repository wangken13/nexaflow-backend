package com.vebcoding.trade.tenant.service;

import com.vebcoding.trade.tenant.api.SubscriptionOrderView;

public interface BillingCheckoutProvider {
    String checkoutUrl(SubscriptionOrderView order);
}
