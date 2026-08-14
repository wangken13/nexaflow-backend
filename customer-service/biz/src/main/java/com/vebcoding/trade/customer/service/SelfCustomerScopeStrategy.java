package com.vebcoding.trade.customer.service;

import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.customer.mapper.CustomerMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SelfCustomerScopeStrategy implements CustomerScopeStrategy {
    public boolean supports(String dataScope) { return "SELF".equals(dataScope); }
    public List<CustomerView> findVisible(CustomerMapper mapper, String tenantId, CustomerAccessProfile profile) {
        return mapper.findByTenantIdAndOwnerId(tenantId, profile.userId());
    }
    public boolean canAccess(CustomerView customer, CustomerAccessProfile profile) {
        return profile.userId().equals(customer.ownerId());
    }
}
