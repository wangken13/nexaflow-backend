package com.vebcoding.trade.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.tenant.api.CreateInvoiceRequest;
import com.vebcoding.trade.tenant.api.CreateSubscriptionOrderRequest;
import com.vebcoding.trade.tenant.api.PlanView;
import com.vebcoding.trade.tenant.api.SubscriptionOrderView;
import com.vebcoding.trade.tenant.mapper.BillingStore;
import com.vebcoding.trade.tenant.service.BillingCallbackVerifier;
import com.vebcoding.trade.tenant.service.BillingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BillingServiceTest {
    private final BillingStore store = mock(BillingStore.class);
    private final BillingCallbackVerifier verifier = mock(BillingCallbackVerifier.class);

    @BeforeEach
    void authenticate() {
        TenantContext.setTenantId("tenant-a");
        TenantContext.setUserId("owner-a");
        TenantContext.setRole("OWNER");
    }

    @AfterEach void clear() { TenantContext.clear(); }

    @Test
    void createsSignedCheckoutOrderFromCatalogPrice() {
        when(store.findPlan("PRO")).thenReturn(Optional.of(new PlanView("PRO", "专业版", 20, 10000,
                3000, BigDecimal.valueOf(9.9))));
        when(store.saveOrder(anyString(), anyString(), any())).thenAnswer(invocation -> invocation.getArgument(2));
        BillingService service = new BillingService(store, order -> "https://pay.example/" + order.id(), verifier,
                new ObjectMapper());

        SubscriptionOrderView order = service.createOrder(new CreateSubscriptionOrderRequest("pro", 12));

        assertThat(order.amount()).isEqualByComparingTo("118.80");
        assertThat(order.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(order.checkoutUrl()).startsWith("https://pay.example/");
    }

    @Test
    void invoiceRequiresPaidOrder() {
        when(store.findOrder("tenant-a", "sub-1")).thenReturn(Optional.of(order("PENDING_PAYMENT")));
        BillingService service = new BillingService(store, ignored -> "", verifier, new ObjectMapper());

        assertThatThrownBy(() -> service.createInvoice("sub-1",
                new CreateInvoiceRequest("示例企业", "91310000", "finance@example.com")))
                .isInstanceOf(BusinessException.class).hasMessage("只有已支付订单可以申请发票");
    }

    @Test
    void signedCallbackDelegatesIdempotentPaymentConfirmation() {
        when(store.markPaidAndUpgrade("sub-1", "txn-1")).thenReturn(order("PAID"));
        BillingService service = new BillingService(store, ignored -> "", verifier, new ObjectMapper());
        String body = "{\"orderId\":\"sub-1\",\"status\":\"PAID\",\"transactionId\":\"txn-1\"}";

        SubscriptionOrderView result = service.handleCallback("123", "signature", body);

        verify(verifier).verify("123", body, "signature");
        verify(store).markPaidAndUpgrade("sub-1", "txn-1");
        assertThat(result.status()).isEqualTo("PAID");
    }

    private SubscriptionOrderView order(String status) {
        Instant now = Instant.now();
        return new SubscriptionOrderView("sub-1", "PRO", "专业版", 12, BigDecimal.valueOf(118.8), status,
                "", status.equals("PAID") ? "txn-1" : "", now.toString(),
                status.equals("PAID") ? now.toString() : "", now.plusSeconds(1800).toString());
    }
}
