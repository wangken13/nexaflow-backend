package com.vebcoding.trade.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CustomerControllerTest {
    @Test
    void listReturnsSeedCustomers() {
        assertThat(new CustomerController().list().data()).isNotEmpty();
    }
}
