package com.vebcoding.trade.inquiry.service;

import com.vebcoding.trade.inquiry.api.InboundInquiryRequest;

interface InboundCustomerResolver {
    ResolvedCustomer resolve(String tenantId, InboundInquiryRequest request);

    record ResolvedCustomer(String customerId, boolean created) {
    }
}
