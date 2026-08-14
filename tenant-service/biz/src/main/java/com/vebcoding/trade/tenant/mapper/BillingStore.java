package com.vebcoding.trade.tenant.mapper;

import com.vebcoding.trade.tenant.api.InvoiceRequestView;
import com.vebcoding.trade.tenant.api.PlanView;
import com.vebcoding.trade.tenant.api.RefundRequestView;
import com.vebcoding.trade.tenant.api.SubscriptionOrderView;
import java.util.List;
import java.util.Optional;

public interface BillingStore {
    List<PlanView> findPlans();
    Optional<PlanView> findPlan(String planCode);
    SubscriptionOrderView saveOrder(String tenantId, String createdBy, SubscriptionOrderView order);
    List<SubscriptionOrderView> findOrders(String tenantId);
    Optional<SubscriptionOrderView> findOrder(String tenantId, String orderId);
    Optional<SubscriptionOrderView> findOrderById(String orderId);
    SubscriptionOrderView markPaidAndUpgrade(String orderId, String transactionId);
    InvoiceRequestView saveInvoice(String tenantId, String createdBy, InvoiceRequestView invoice);
    List<InvoiceRequestView> findInvoices(String tenantId);
    RefundRequestView saveRefund(String tenantId, String createdBy, RefundRequestView refund);
    List<RefundRequestView> findRefunds(String tenantId);
    boolean hasOpenRefund(String tenantId, String orderId);
}
