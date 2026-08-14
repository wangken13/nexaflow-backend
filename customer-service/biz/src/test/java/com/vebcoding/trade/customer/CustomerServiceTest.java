package com.vebcoding.trade.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.AccessDeniedException;
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
import com.vebcoding.trade.customer.service.AllCustomerScopeStrategy;
import com.vebcoding.trade.customer.service.CustomerAccessPolicy;
import com.vebcoding.trade.customer.service.DepartmentCustomerScopeStrategy;
import com.vebcoding.trade.customer.service.RoleBasedSensitiveDataMasker;
import com.vebcoding.trade.customer.service.SelfCustomerScopeStrategy;
import com.vebcoding.trade.common.ImportJobRecorder;

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
        CustomerService service = service();

        var customer = service.create(new CreateCustomerRequest("  Acme Trading  ", "  USA  ", "  vip  "));

        assertThat(customer.name()).isEqualTo("Acme Trading");
        assertThat(customer.country()).isEqualTo("USA");
        assertThat(customer.tag()).isEqualTo("vip");
    }

    @Test
    void createRejectsBlankName() {
        CustomerService service = service();

        assertThatThrownBy(() -> service.create(new CreateCustomerRequest(" ", "USA", "vip")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("客户名称不能为空");
    }

    @Test
    void customerDetailContainsContactsAndTimeline() {
        CustomerService service = service();
        var customer = service.create(new CreateCustomerRequest("Acme Trading", "USA", "vip"));

        service.addContact(customer.id(), new CreateContactRequest("Amanda", "a@example.com", "123", "Buyer", true));
        service.addFollowup(customer.id(), new CreateFollowupRequest("EMAIL", "Confirmed annual demand", "Ken"));

        var detail = service.detail(customer.id());
        assertThat(detail.contacts()).singleElement().satisfies(contact -> assertThat(contact.primary()).isTrue());
        assertThat(detail.timeline()).singleElement().satisfies(item -> assertThat(item.type()).isEqualTo("EMAIL"));
    }

    @Test
    void bulkImportSkipsDuplicateAndInvalidCustomers() {
        CustomerService service = service();
        var result = service.bulkImport(List.of(
                new CreateCustomerRequest("Acme", "US", "vip"),
                new CreateCustomerRequest("Acme", "US", "vip"),
                new CreateCustomerRequest("", "DE", "new")));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(2);
        assertThat(result.errors()).hasSize(2);
    }

    @Test
    void exportRequiresAdministrativePermission() {
        CustomerService service = service();
        TenantContext.setRole("SALES");

        assertThatThrownBy(service::exportData).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void salesOnlySeesOwnedCustomers() {
        CustomerService service = service();
        TenantContext.setRole("SALES");
        TenantContext.setUserId("sales-a");

        var mine = service.create(new CreateCustomerRequest("My Customer", "US", "new"));
        TenantContext.setUserId("sales-b");
        service.create(new CreateCustomerRequest("Other Customer", "DE", "new"));
        TenantContext.setUserId("sales-a");

        assertThat(service.list()).extracting("id").containsExactly(mine.id());
    }

    @Test
    void viewerReceivesMaskedContactDetails() {
        CustomerService service = service();
        var customer = service.create(new CreateCustomerRequest("Acme", "US", "vip"));
        service.addContact(customer.id(), new CreateContactRequest("Amanda", "amanda@example.com",
                "13812345678", "Buyer", true));
        TenantContext.setRole("VIEWER");

        var contact = service.detail(customer.id()).contacts().getFirst();
        assertThat(contact.email()).isEqualTo("a***@example.com");
        assertThat(contact.phone()).isEqualTo("138****5678");
    }

    private CustomerService service() {
        InMemoryCustomerMapper mapper = new InMemoryCustomerMapper();
        CustomerAccessPolicy policy = new CustomerAccessPolicy(mapper, List.of(new AllCustomerScopeStrategy(),
                new DepartmentCustomerScopeStrategy(), new SelfCustomerScopeStrategy()));
        return new CustomerService(mapper, policy, new RoleBasedSensitiveDataMasker(),
                ImportJobRecorder.passthrough());
    }
}
