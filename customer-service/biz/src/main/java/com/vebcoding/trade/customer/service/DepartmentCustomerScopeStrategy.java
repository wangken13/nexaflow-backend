package com.vebcoding.trade.customer.service;

import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.customer.mapper.CustomerMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DepartmentCustomerScopeStrategy implements CustomerScopeStrategy {
    public boolean supports(String dataScope) { return "DEPARTMENT".equals(dataScope); }
    public List<CustomerView> findVisible(CustomerMapper mapper, String tenantId, CustomerAccessProfile profile) {
        return profile.departmentId().isBlank() ? List.of()
                : mapper.findByTenantIdAndDepartmentId(tenantId, profile.departmentId());
    }
    public boolean canAccess(CustomerView customer, CustomerAccessProfile profile) {
        return !profile.departmentId().isBlank() && profile.departmentId().equals(customer.departmentId());
    }
}
