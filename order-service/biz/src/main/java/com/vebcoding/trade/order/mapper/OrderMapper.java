package com.vebcoding.trade.order.mapper;

import com.vebcoding.trade.order.api.OrderView;
import java.util.List;
import java.util.Optional;

public interface OrderMapper {
    List<OrderView> findByTenantId(String tenantId);

    Optional<OrderView> findByTenantIdAndId(String tenantId, String id);

    OrderView save(OrderView order);
}
