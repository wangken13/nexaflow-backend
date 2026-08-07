package com.vebcoding.trade.customer.mapper;

import com.vebcoding.trade.customer.api.CustomerView;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCustomerMapper implements CustomerMapper {
    private final List<CustomerView> customers = new CopyOnWriteArrayList<>(List.of(
            new CustomerView("cus-001", "demo-tenant", "North Star Imports", "USA", "vip", Instant.now().toString()),
            new CustomerView("cus-002", "demo-tenant", "Atlas Homeware", "Germany", "new", Instant.now().toString())));

    @Override
    public List<CustomerView> findByTenantId(String tenantId) {
        return customers.stream().filter(item -> tenantId.equals(item.tenantId())).toList();
    }

    @Override
    public CustomerView save(CustomerView customer) {
        customers.removeIf(item -> item.id().equals(customer.id()));
        customers.add(customer);
        return customer;
    }
}