package com.vebcoding.trade.customer.mapper;

import com.vebcoding.trade.customer.api.CustomerView;
import java.util.List;

public interface CustomerMapper {
    List<CustomerView> findByTenantId(String tenantId);

    CustomerView save(CustomerView customer);
}