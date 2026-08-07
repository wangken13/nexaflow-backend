package com.vebcoding.trade.order.mapper;

import com.vebcoding.trade.order.api.OrderView;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryOrderMapper implements OrderMapper {
    private final List<OrderView> orders = new CopyOnWriteArrayList<>(List.of(
            new OrderView("ord-001", "demo-tenant", "North Star Imports", "Ceramic mugs", "PRODUCTION",
                    LocalDate.now().plusDays(7).toString(), false)));

    @Override
    public List<OrderView> findByTenantId(String tenantId) {
        return orders.stream().filter(item -> tenantId.equals(item.tenantId())).toList();
    }

    @Override
    public OrderView save(OrderView order) {
        orders.removeIf(item -> item.id().equals(order.id()));
        orders.add(order);
        return order;
    }
}