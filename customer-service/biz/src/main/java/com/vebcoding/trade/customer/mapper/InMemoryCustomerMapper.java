package com.vebcoding.trade.customer.mapper;

import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.customer.api.ContactView;
import com.vebcoding.trade.customer.api.FollowupView;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.customer.service.AssignableOwner;
import com.vebcoding.trade.customer.service.CustomerAccessProfile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryCustomerMapper implements CustomerMapper {
    private final List<CustomerView> customers = new CopyOnWriteArrayList<>(List.of(
            new CustomerView("cus-001", "demo-tenant", "North Star Imports", "USA", "vip", Instant.now().toString()),
            new CustomerView("cus-002", "demo-tenant", "Atlas Homeware", "Germany", "new", Instant.now().toString())));
    private final List<ContactView> contacts = new CopyOnWriteArrayList<>();
    private final List<FollowupView> followups = new CopyOnWriteArrayList<>();

    @Override
    public List<CustomerView> findByTenantId(String tenantId) {
        return customers.stream().filter(item -> tenantId.equals(item.tenantId())).toList();
    }

    public List<CustomerView> findByTenantIdAndOwnerId(String tenantId, String ownerId) {
        return customers.stream().filter(item -> tenantId.equals(item.tenantId()) && ownerId.equals(item.ownerId())).toList();
    }

    public List<CustomerView> findByTenantIdAndDepartmentId(String tenantId, String departmentId) {
        return customers.stream().filter(item -> tenantId.equals(item.tenantId())
                && departmentId.equals(item.departmentId())).toList();
    }

    public CustomerAccessProfile findAccessProfile(String tenantId, String userId, String fallbackRole) {
        String scope = "OWNER".equals(fallbackRole) || "ADMIN".equals(fallbackRole) ? "ALL"
                : "OPERATOR".equals(fallbackRole) ? "DEPARTMENT" : "SELF";
        return new CustomerAccessProfile(userId, fallbackRole, scope, "dep-sales");
    }

    public Optional<AssignableOwner> findAssignableOwner(String tenantId, String userId) {
        return Optional.of(new AssignableOwner(userId, userId, "dep-sales", "销售部"));
    }

    @Override
    public CustomerView save(CustomerView customer) {
        customers.removeIf(item -> item.id().equals(customer.id()));
        customers.add(customer);
        return customer;
    }

    public Optional<CustomerView> findByTenantIdAndId(String tenantId, String id) {
        return customers.stream().filter(item -> tenantId.equals(item.tenantId()) && id.equals(item.id())).findFirst();
    }
    public boolean delete(String tenantId, String id) {
        contacts.removeIf(item -> id.equals(item.customerId()));
        followups.removeIf(item -> id.equals(item.customerId()));
        return customers.removeIf(item -> tenantId.equals(item.tenantId()) && id.equals(item.id()));
    }
    public List<ContactView> findContacts(String tenantId, String customerId) {
        return contacts.stream().filter(item -> customerId.equals(item.customerId())).toList();
    }
    public ContactView saveContact(String tenantId, ContactView contact) { contacts.add(contact); return contact; }
    public List<FollowupView> findFollowups(String tenantId, String customerId) {
        return followups.stream().filter(item -> customerId.equals(item.customerId())).toList();
    }
    public FollowupView saveFollowup(String tenantId, FollowupView followup) { followups.add(followup); return followup; }
}
