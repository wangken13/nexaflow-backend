package com.vebcoding.trade.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.customer.controller.CustomerController;
import com.vebcoding.trade.customer.mapper.InMemoryCustomerMapper;
import com.vebcoding.trade.customer.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class CustomerControllerTest {
    @BeforeEach
    void authenticate() {
        TenantContext.setTenantId("demo-tenant");
        TenantContext.setUserId("admin");
        TenantContext.setRole("OWNER");
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void listReturnsSeedCustomers() {
        CustomerService service = new CustomerService(new InMemoryCustomerMapper());

        assertThat(new CustomerController(service).list().data()).isNotEmpty();
    }
}
