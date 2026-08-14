package com.vebcoding.trade.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.customer.controller.CustomerController;
import com.vebcoding.trade.customer.mapper.InMemoryCustomerMapper;
import com.vebcoding.trade.customer.service.CustomerService;
import com.vebcoding.trade.customer.service.AllCustomerScopeStrategy;
import com.vebcoding.trade.customer.service.CustomerAccessPolicy;
import com.vebcoding.trade.customer.service.DepartmentCustomerScopeStrategy;
import com.vebcoding.trade.customer.service.RoleBasedSensitiveDataMasker;
import com.vebcoding.trade.customer.service.SelfCustomerScopeStrategy;
import com.vebcoding.trade.common.ImportJobRecorder;
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
        InMemoryCustomerMapper mapper = new InMemoryCustomerMapper();
        CustomerAccessPolicy policy = new CustomerAccessPolicy(mapper, java.util.List.of(
                new AllCustomerScopeStrategy(), new DepartmentCustomerScopeStrategy(),
                new SelfCustomerScopeStrategy()));
        CustomerService service = new CustomerService(mapper, policy, new RoleBasedSensitiveDataMasker(),
                ImportJobRecorder.passthrough());

        assertThat(new CustomerController(service).list().data()).isNotEmpty();
    }
}
