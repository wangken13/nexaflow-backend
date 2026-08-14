package com.vebcoding.trade.tenant.mapper;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.tenant.api.InvoiceRequestView;
import com.vebcoding.trade.tenant.api.PlanView;
import com.vebcoding.trade.tenant.api.RefundRequestView;
import com.vebcoding.trade.tenant.api.SubscriptionOrderView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBillingStore implements BillingStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcBillingStore(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public List<PlanView> findPlans() {
        return jdbcTemplate.query("""
                SELECT plan_code, plan_name, member_limit, customer_limit, ai_credit_limit, monthly_price
                FROM plan_catalog WHERE active_flag=1 ORDER BY monthly_price
                """, (rs, rowNum) -> new PlanView(rs.getString("plan_code"), rs.getString("plan_name"),
                rs.getInt("member_limit"), rs.getInt("customer_limit"), rs.getInt("ai_credit_limit"),
                rs.getBigDecimal("monthly_price")));
    }

    public Optional<PlanView> findPlan(String planCode) {
        return findPlans().stream().filter(item -> item.planCode().equals(planCode)).findFirst();
    }

    public SubscriptionOrderView saveOrder(String tenantId, String createdBy, SubscriptionOrderView order) {
        jdbcTemplate.update("""
                INSERT INTO subscription_orders
                  (id, tenant_id, plan_code, billing_months, amount, status, provider_reference,
                   created_by, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, order.id(), tenantId, order.planCode(), order.billingMonths(), order.amount(), order.status(),
                emptyToNull(order.checkoutUrl()), createdBy, timestamp(order.createdAt()), timestamp(order.expiresAt()));
        return order;
    }

    public List<SubscriptionOrderView> findOrders(String tenantId) {
        return jdbcTemplate.query("""
                SELECT orders.id, orders.plan_code, plan.plan_name, orders.billing_months, orders.amount,
                       orders.status, orders.provider_reference, orders.provider_transaction_id,
                       orders.created_at, orders.paid_at, orders.expires_at
                FROM subscription_orders orders JOIN plan_catalog plan ON plan.plan_code=orders.plan_code
                WHERE orders.tenant_id=? ORDER BY orders.created_at DESC
                """, (rs, rowNum) -> order(rs), tenantId);
    }

    public Optional<SubscriptionOrderView> findOrder(String tenantId, String orderId) {
        return findOrders(tenantId).stream().filter(item -> item.id().equals(orderId)).findFirst();
    }

    public Optional<SubscriptionOrderView> findOrderById(String orderId) {
        return jdbcTemplate.query("""
                SELECT orders.id, orders.plan_code, plan.plan_name, orders.billing_months, orders.amount,
                       orders.status, orders.provider_reference, orders.provider_transaction_id,
                       orders.created_at, orders.paid_at, orders.expires_at
                FROM subscription_orders orders JOIN plan_catalog plan ON plan.plan_code=orders.plan_code
                WHERE orders.id=?
                """, (rs, rowNum) -> order(rs), orderId).stream().findFirst();
    }

    public SubscriptionOrderView markPaidAndUpgrade(String orderId, String transactionId) {
        int updated = jdbcTemplate.update("""
                UPDATE subscription_orders SET status='PAID', provider_transaction_id=?, paid_at=CURRENT_TIMESTAMP
                WHERE id=? AND status IN ('PENDING_PAYMENT', 'PAYMENT_CONFIGURATION_REQUIRED')
                """, transactionId, orderId);
        SubscriptionOrderView order = findOrderById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订阅订单不存在"));
        if (updated == 1) {
            jdbcTemplate.update("""
                    UPDATE tenants tenant JOIN subscription_orders orders ON orders.tenant_id=tenant.id
                    SET tenant.plan_code=orders.plan_code WHERE orders.id=?
                    """, orderId);
        }
        return findOrderById(orderId).orElseThrow();
    }

    public InvoiceRequestView saveInvoice(String tenantId, String createdBy, InvoiceRequestView invoice) {
        jdbcTemplate.update("""
                INSERT INTO invoice_requests
                  (id, tenant_id, order_id, invoice_title, tax_number, recipient_email, status, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, invoice.id(), tenantId, invoice.orderId(), invoice.invoiceTitle(), emptyToNull(invoice.taxNumber()),
                invoice.recipientEmail(), invoice.status(), createdBy, timestamp(invoice.createdAt()));
        return invoice;
    }

    public List<InvoiceRequestView> findInvoices(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id, order_id, invoice_title, tax_number, recipient_email, status, created_at
                FROM invoice_requests WHERE tenant_id=? ORDER BY created_at DESC
                """, (rs, rowNum) -> new InvoiceRequestView(rs.getString("id"), rs.getString("order_id"),
                rs.getString("invoice_title"), value(rs.getString("tax_number")), rs.getString("recipient_email"),
                rs.getString("status"), rs.getTimestamp("created_at").toInstant().toString()), tenantId);
    }

    public RefundRequestView saveRefund(String tenantId, String createdBy, RefundRequestView refund) {
        jdbcTemplate.update("""
                INSERT INTO refund_requests (id, tenant_id, order_id, reason, status, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, refund.id(), tenantId, refund.orderId(), refund.reason(), refund.status(), createdBy,
                timestamp(refund.createdAt()));
        return refund;
    }

    public List<RefundRequestView> findRefunds(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id, order_id, reason, status, created_at FROM refund_requests
                WHERE tenant_id=? ORDER BY created_at DESC
                """, (rs, rowNum) -> new RefundRequestView(rs.getString("id"), rs.getString("order_id"),
                rs.getString("reason"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant().toString()), tenantId);
    }

    public boolean hasOpenRefund(String tenantId, String orderId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM refund_requests
                WHERE tenant_id=? AND order_id=? AND status IN ('SUBMITTED', 'PROCESSING')
                """, Integer.class, tenantId, orderId);
        return count != null && count > 0;
    }

    private SubscriptionOrderView order(ResultSet rs) throws SQLException {
        return new SubscriptionOrderView(rs.getString("id"), rs.getString("plan_code"), rs.getString("plan_name"),
                rs.getInt("billing_months"), rs.getBigDecimal("amount"), rs.getString("status"),
                value(rs.getString("provider_reference")), value(rs.getString("provider_transaction_id")),
                rs.getTimestamp("created_at").toInstant().toString(), instant(rs.getTimestamp("paid_at")),
                rs.getTimestamp("expires_at").toInstant().toString());
    }

    private Timestamp timestamp(String instant) { return Timestamp.from(Instant.parse(instant)); }
    private String instant(Timestamp value) { return value == null ? "" : value.toInstant().toString(); }
    private String value(String value) { return value == null ? "" : value; }
    private String emptyToNull(String value) { return value == null || value.isBlank() ? null : value; }
}
