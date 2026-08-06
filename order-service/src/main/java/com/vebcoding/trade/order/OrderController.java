package com.vebcoding.trade.order;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.common.TenantContext;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderController {
    private final List<OrderView> orders = new CopyOnWriteArrayList<>(List.of(
            new OrderView("ord-001", "demo-tenant", "North Star Imports", "Ceramic mugs", "PRODUCTION", LocalDate.now().plusDays(7).toString(), false)));

    @GetMapping
    public ApiResponse<List<OrderView>> list() {
        String tenantId = TenantContext.tenantId();
        return ApiResponse.ok(orders.stream().filter(item -> tenantId.equals(item.tenantId())).toList());
    }

    @GetMapping("/exceptions")
    public ApiResponse<List<OrderView>> exceptions() {
        return ApiResponse.ok(orders.stream().filter(OrderView::risk).toList());
    }

    @PostMapping
    public ApiResponse<OrderView> create(@RequestBody CreateOrderRequest request) {
        boolean risk = LocalDate.parse(request.deliveryDate()).isBefore(LocalDate.now().plusDays(3));
        OrderView order = new OrderView("ord-" + UUID.randomUUID(), TenantContext.tenantId(), request.customerName(),
                request.productName(), "CONFIRMED", request.deliveryDate(), risk);
        orders.add(order);
        return ApiResponse.ok(order);
    }

    public record CreateOrderRequest(@NotBlank String customerName, @NotBlank String productName, @NotBlank String deliveryDate) {
    }

    public record OrderView(String id, String tenantId, String customerName, String productName, String status,
                            String deliveryDate, boolean risk) {
    }
}
