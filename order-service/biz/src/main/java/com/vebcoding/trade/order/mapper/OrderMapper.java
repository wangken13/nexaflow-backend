package com.vebcoding.trade.order.mapper;

import com.vebcoding.trade.order.api.OrderView;
import java.util.List;

public interface OrderMapper {
    List<OrderView> findByTenantId(String tenantId);

    OrderView save(OrderView order);
}