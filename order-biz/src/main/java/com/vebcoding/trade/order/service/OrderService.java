package com.vebcoding.trade.order.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.order.api.CreateOrderRequest;
import com.vebcoding.trade.order.api.OrderView;
import com.vebcoding.trade.order.mapper.OrderMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public List<OrderView> list() {
        return orderMapper.findByTenantId(TenantContext.tenantId());
    }

    public List<OrderView> exceptions() {
        return orderMapper.findByTenantId(TenantContext.tenantId()).stream().filter(OrderView::risk).toList();
    }

    public OrderView create(CreateOrderRequest request) {
        boolean risk = LocalDate.parse(request.deliveryDate()).isBefore(LocalDate.now().plusDays(3));
        OrderView order = new OrderView("ord-" + UUID.randomUUID(), TenantContext.tenantId(), request.customerName(),
                request.productName(), "CONFIRMED", request.deliveryDate(), risk);
        return orderMapper.save(order);
    }
}