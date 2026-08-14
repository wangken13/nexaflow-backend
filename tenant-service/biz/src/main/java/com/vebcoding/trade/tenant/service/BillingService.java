package com.vebcoding.trade.tenant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.tenant.api.CreateInvoiceRequest;
import com.vebcoding.trade.tenant.api.CreateRefundRequest;
import com.vebcoding.trade.tenant.api.CreateSubscriptionOrderRequest;
import com.vebcoding.trade.tenant.api.InvoiceRequestView;
import com.vebcoding.trade.tenant.api.PlanView;
import com.vebcoding.trade.tenant.api.RefundRequestView;
import com.vebcoding.trade.tenant.api.SubscriptionOrderView;
import com.vebcoding.trade.tenant.mapper.BillingStore;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {
    private final BillingStore store;
    private final BillingCheckoutProvider checkoutProvider;
    private final BillingCallbackVerifier callbackVerifier;
    private final ObjectMapper objectMapper;

    public BillingService(BillingStore store, BillingCheckoutProvider checkoutProvider,
                          BillingCallbackVerifier callbackVerifier, ObjectMapper objectMapper) {
        this.store = store;
        this.checkoutProvider = checkoutProvider;
        this.callbackVerifier = callbackVerifier;
        this.objectMapper = objectMapper;
    }

    public List<PlanView> plans() { RoleGuard.requireAny("OWNER", "ADMIN"); return store.findPlans(); }
    public List<SubscriptionOrderView> orders() { RoleGuard.requireAny("OWNER", "ADMIN"); return store.findOrders(TenantContext.tenantId()); }
    public List<InvoiceRequestView> invoices() { RoleGuard.requireAny("OWNER", "ADMIN"); return store.findInvoices(TenantContext.tenantId()); }
    public List<RefundRequestView> refunds() { RoleGuard.requireAny("OWNER", "ADMIN"); return store.findRefunds(TenantContext.tenantId()); }

    @Transactional
    public SubscriptionOrderView createOrder(CreateSubscriptionOrderRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        String planCode = TextSanitizer.required(request.planCode(), "套餐").toUpperCase(Locale.ROOT);
        PlanView plan = store.findPlan(planCode).orElseThrow(() -> BusinessException.notFound("套餐不存在或已下架"));
        Instant now = Instant.now();
        SubscriptionOrderView draft = new SubscriptionOrderView("sub-" + UUID.randomUUID(), plan.planCode(),
                plan.planName(), request.billingMonths(), plan.monthlyPrice().multiply(java.math.BigDecimal.valueOf(request.billingMonths())),
                "PENDING_PAYMENT", "", "", now.toString(), "", now.plus(30, ChronoUnit.MINUTES).toString());
        String checkoutUrl = checkoutProvider.checkoutUrl(draft);
        SubscriptionOrderView order = new SubscriptionOrderView(draft.id(), draft.planCode(), draft.planName(),
                draft.billingMonths(), draft.amount(), checkoutUrl.isBlank() ? "PAYMENT_CONFIGURATION_REQUIRED" : "PENDING_PAYMENT",
                checkoutUrl, "", draft.createdAt(), "", draft.expiresAt());
        return store.saveOrder(TenantContext.tenantId(), TenantContext.userId(), order);
    }

    public InvoiceRequestView createInvoice(String orderId, CreateInvoiceRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        SubscriptionOrderView order = requireOrder(orderId);
        if (!order.status().equals("PAID")) throw BusinessException.conflict("只有已支付订单可以申请发票");
        InvoiceRequestView invoice = new InvoiceRequestView("inv-" + UUID.randomUUID(), orderId,
                TextSanitizer.required(request.invoiceTitle(), "发票抬头"), TextSanitizer.optional(request.taxNumber()),
                TextSanitizer.required(request.recipientEmail(), "接收邮箱"), "SUBMITTED", Instant.now().toString());
        return store.saveInvoice(TenantContext.tenantId(), TenantContext.userId(), invoice);
    }

    public RefundRequestView createRefund(String orderId, CreateRefundRequest request) {
        RoleGuard.requireAny("OWNER");
        SubscriptionOrderView order = requireOrder(orderId);
        if (!order.status().equals("PAID")) throw BusinessException.conflict("只有已支付订单可以申请退款");
        if (store.hasOpenRefund(TenantContext.tenantId(), orderId)) throw BusinessException.conflict("该订单已有处理中退款申请");
        RefundRequestView refund = new RefundRequestView("ref-" + UUID.randomUUID(), orderId,
                TextSanitizer.required(request.reason(), "退款原因"), "SUBMITTED", Instant.now().toString());
        return store.saveRefund(TenantContext.tenantId(), TenantContext.userId(), refund);
    }

    @Transactional
    public SubscriptionOrderView handleCallback(String timestamp, String signature, String body) {
        callbackVerifier.verify(timestamp, body, signature);
        PaymentCallback callback;
        try { callback = objectMapper.readValue(body, PaymentCallback.class); }
        catch (Exception exception) { throw new BusinessException("支付回调内容不是有效的 JSON 数据"); }
        if (!"PAID".equals(callback.status())) throw new BusinessException("暂不支持的支付状态");
        return store.markPaidAndUpgrade(TextSanitizer.required(callback.orderId(), "订单编号"),
                TextSanitizer.required(callback.transactionId(), "支付流水号"));
    }

    private SubscriptionOrderView requireOrder(String orderId) {
        return store.findOrder(TenantContext.tenantId(), orderId)
                .orElseThrow(() -> BusinessException.notFound("订阅订单不存在"));
    }

    private record PaymentCallback(String orderId, String status, String transactionId) { }
}
