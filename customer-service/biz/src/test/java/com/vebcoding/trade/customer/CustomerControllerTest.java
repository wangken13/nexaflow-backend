package com.vebcoding.trade.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.customer.controller.CustomerController;
import com.vebcoding.trade.customer.mapper.InMemoryCustomerMapper;
import com.vebcoding.trade.customer.service.CustomerService;
import org.junit.jupiter.api.Test;

class CustomerControllerTest {
    @Test
    void listReturnsSeedCustomers() {
        CustomerService service = new CustomerService(new InMemoryCustomerMapper());

        assertThat(new CustomerController(service).list().data()).isNotEmpty();
    }
}
