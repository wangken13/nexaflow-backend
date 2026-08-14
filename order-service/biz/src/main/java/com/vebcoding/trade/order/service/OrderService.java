package com.vebcoding.trade.order.service;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.order.api.CreateOrderRequest;
import com.vebcoding.trade.order.api.OrderView;
import com.vebcoding.trade.order.mapper.OrderMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "RISK_REVIEW", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("PRODUCING", "CANCELLED"),
            "PRODUCING", Set.of("READY_TO_SHIP", "CANCELLED"),
            "READY_TO_SHIP", Set.of("SHIPPED", "CANCELLED"),
            "SHIPPED", Set.of("DELIVERED"),
            "DELIVERED", Set.of(),
            "CANCELLED", Set.of());
    private final OrderMapper orderMapper;
    private final OrderReferenceService referenceService;

    public OrderService(OrderMapper orderMapper, OrderReferenceService referenceService) {
        this.orderMapper = orderMapper;
        this.referenceService = referenceService;
    }

    public List<OrderView> list() {
        return orderMapper.findByTenantId(TenantContext.tenantId());
    }

    public List<OrderView> exportData() {
        RoleGuard.requireAny("OWNER", "ADMIN");
        return orderMapper.findByTenantId(TenantContext.tenantId());
    }

    public List<OrderView> exceptions() {
        return orderMapper.findByTenantId(TenantContext.tenantId()).stream().filter(OrderView::risk).toList();
    }

    public OrderView create(CreateOrderRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES", "OPERATOR");
        String customerId = TextSanitizer.required(request.customerId(), "客户");
        String productId = TextSanitizer.required(request.productId(), "产品");
        OrderReferenceService.OrderReference reference = referenceService.resolve(customerId, productId);
        LocalDate deliveryDate = parseDeliveryDate(request.deliveryDate());
        boolean risk = deliveryDate.isBefore(LocalDate.now().plusDays(3));
        String status = risk ? "RISK_REVIEW" : "CONFIRMED";
        OrderView order = new OrderView("ord-" + UUID.randomUUID(), TenantContext.tenantId(),
                reference.customerId(), reference.customerName(), reference.productId(), reference.productName(),
                status, deliveryDate.toString(), risk);
        return orderMapper.save(order);
    }

    public OrderView updateStatus(String id, String status) {
        RoleGuard.requireAny("OWNER", "ADMIN", "OPERATOR");
        String normalized = TextSanitizer.required(status, "订单状态").toUpperCase();
        OrderView current = orderMapper.findByTenantIdAndId(TenantContext.tenantId(), id)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (!TRANSITIONS.getOrDefault(current.status(), Set.of()).contains(normalized)) {
            throw BusinessException.conflict("订单不能从 " + current.status() + " 变更为 " + normalized);
        }
        boolean risk = normalized.equals("RISK_REVIEW") || (!normalized.equals("DELIVERED")
                && !normalized.equals("CANCELLED") && LocalDate.parse(current.deliveryDate()).isBefore(LocalDate.now()));
        return orderMapper.save(new OrderView(current.id(), current.tenantId(), current.customerId(),
                current.customerName(), current.productId(), current.productName(), normalized,
                current.deliveryDate(), risk));
    }

    private LocalDate parseDeliveryDate(String deliveryDate) {
        try {
            return LocalDate.parse(TextSanitizer.required(deliveryDate, "交付日期"));
        } catch (Exception ex) {
            throw new BusinessException("交付日期格式必须为 yyyy-MM-dd");
        }
    }
}
