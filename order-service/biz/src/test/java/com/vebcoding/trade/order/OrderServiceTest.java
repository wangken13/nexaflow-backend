package com.vebcoding.trade.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.AccessDeniedException;
import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.customer.api.CustomerDetailView;
import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.order.api.CreateOrderRequest;
import com.vebcoding.trade.order.client.OrderCustomerClient;
import com.vebcoding.trade.order.client.OrderProductClient;
import com.vebcoding.trade.order.mapper.InMemoryOrderMapper;
import com.vebcoding.trade.order.service.OrderReferenceService;
import com.vebcoding.trade.order.service.OrderService;
import com.vebcoding.trade.product.api.ProductView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class OrderServiceTest {
    @BeforeEach
    void authenticate() {
        TenantContext.setTenantId("demo-tenant");
        TenantContext.setUserId("admin");
        TenantContext.setRole("OWNER");
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void createFlagsNearDeliveryOrderAsRisk() {
        OrderService service = service();

        var order = service.create(new CreateOrderRequest("cus-001", "prd-001", LocalDate.now().plusDays(1).toString()));

        assertThat(order.risk()).isTrue();
        assertThat(order.status()).isEqualTo("RISK_REVIEW");
        assertThat(order.customerName()).isEqualTo("Acme");
        assertThat(order.productName()).isEqualTo("Mug");
    }

    @Test
    void createRejectsInvalidDeliveryDate() {
        OrderService service = service();

        assertThatThrownBy(() -> service.create(new CreateOrderRequest("cus-001", "prd-001", "tomorrow")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("交付日期格式必须为 yyyy-MM-dd");
    }

    @Test
    void orderFollowsDeliveryWorkflow() {
        OrderService service = service();
        var order = service.create(new CreateOrderRequest("cus-001", "prd-001", LocalDate.now().plusDays(20).toString()));

        assertThat(service.updateStatus(order.id(), "PRODUCING").status()).isEqualTo("PRODUCING");
        assertThat(service.updateStatus(order.id(), "READY_TO_SHIP").status()).isEqualTo("READY_TO_SHIP");
        assertThat(service.updateStatus(order.id(), "SHIPPED").status()).isEqualTo("SHIPPED");
        assertThat(service.updateStatus(order.id(), "DELIVERED").status()).isEqualTo("DELIVERED");
    }

    @Test
    void createRejectsInactiveCatalogProduct() {
        OrderService service = service(false);

        assertThatThrownBy(() -> service.create(new CreateOrderRequest(
                "cus-001", "prd-001", LocalDate.now().plusDays(20).toString())))
                .isInstanceOf(BusinessException.class)
                .hasMessage("所选产品已停用");
    }

    @Test
    void exportRequiresAdministrativePermission() {
        OrderService service = service();
        TenantContext.setRole("SALES");

        assertThatThrownBy(service::exportData).isInstanceOf(AccessDeniedException.class);
    }

    private OrderService service() {
        return service(true);
    }

    private OrderService service(boolean productActive) {
        OrderCustomerClient customerClient = id -> ApiResponse.ok(new CustomerDetailView(
                new CustomerView(id, "demo-tenant", "Acme", "US", "vip", "2026-08-09T00:00:00Z"),
                List.of(), List.of()));
        OrderProductClient productClient = id -> ApiResponse.ok(new ProductView(id, "demo-tenant", "SKU-1", "Mug",
                "12oz", "USD", BigDecimal.TEN, 100, productActive, "2026-08-09T00:00:00Z"));
        return new OrderService(new InMemoryOrderMapper(), new OrderReferenceService(customerClient, productClient));
    }
}
