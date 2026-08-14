package com.vebcoding.trade.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.customer.api.CreateCustomerRequest;
import com.vebcoding.trade.customer.api.CreateContactRequest;
import com.vebcoding.trade.customer.api.CreateFollowupRequest;
import com.vebcoding.trade.customer.mapper.InMemoryCustomerMapper;
import com.vebcoding.trade.customer.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;

class CustomerServiceTest {
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
    void createNormalizesCustomerFields() {
        CustomerService service = new CustomerService(new InMemoryCustomerMapper());

        var customer = service.create(new CreateCustomerRequest("  Acme Trading  ", "  USA  ", "  vip  "));

        assertThat(customer.name()).isEqualTo("Acme Trading");
        assertThat(customer.country()).isEqualTo("USA");
        assertThat(customer.tag()).isEqualTo("vip");
    }

    @Test
    void createRejectsBlankName() {
        CustomerService service = new CustomerService(new InMemoryCustomerMapper());

        assertThatThrownBy(() -> service.create(new CreateCustomerRequest(" ", "USA", "vip")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("客户名称不能为空");
    }

    @Test
    void customerDetailContainsContactsAndTimeline() {
        CustomerService service = new CustomerService(new InMemoryCustomerMapper());
        var customer = service.create(new CreateCustomerRequest("Acme Trading", "USA", "vip"));

        service.addContact(customer.id(), new CreateContactRequest("Amanda", "a@example.com", "123", "Buyer", true));
        service.addFollowup(customer.id(), new CreateFollowupRequest("EMAIL", "Confirmed annual demand", "Ken"));

        var detail = service.detail(customer.id());
        assertThat(detail.contacts()).singleElement().satisfies(contact -> assertThat(contact.primary()).isTrue());
        assertThat(detail.timeline()).singleElement().satisfies(item -> assertThat(item.type()).isEqualTo("EMAIL"));
    }

    @Test
    void bulkImportSkipsDuplicateAndInvalidCustomers() {
        CustomerService service = new CustomerService(new InMemoryCustomerMapper());
        var result = service.bulkImport(List.of(
                new CreateCustomerRequest("Acme", "US", "vip"),
                new CreateCustomerRequest("Acme", "US", "vip"),
                new CreateCustomerRequest("", "DE", "new")));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(2);
        assertThat(result.errors()).hasSize(2);
    }
}
