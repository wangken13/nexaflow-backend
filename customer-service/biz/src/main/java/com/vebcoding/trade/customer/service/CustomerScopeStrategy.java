package com.vebcoding.trade.customer.service;

import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.customer.mapper.CustomerMapper;
import java.util.List;

public interface CustomerScopeStrategy {
    boolean supports(String dataScope);
    List<CustomerView> findVisible(CustomerMapper mapper, String tenantId, CustomerAccessProfile profile);
    boolean canAccess(CustomerView customer, CustomerAccessProfile profile);
}
