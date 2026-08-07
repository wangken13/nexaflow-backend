package com.vebcoding.trade.customer.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.customer.api.CreateCustomerRequest;
import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.customer.mapper.CustomerMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    public List<CustomerView> list() {
        return customerMapper.findByTenantId(TenantContext.tenantId());
    }

    public CustomerView create(CreateCustomerRequest request) {
        CustomerView customer = new CustomerView("cus-" + UUID.randomUUID(), TenantContext.tenantId(),
                request.name(), request.country(), request.tag(), Instant.now().toString());
        return customerMapper.save(customer);
    }

    public List<String> tags() {
        return List.of("new", "vip", "at-risk", "quoted");
    }
}