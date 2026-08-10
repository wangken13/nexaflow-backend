package com.vebcoding.trade.customer.mapper;

import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.customer.api.ContactView;
import com.vebcoding.trade.customer.api.FollowupView;
import java.util.List;
import java.util.Optional;

public interface CustomerMapper {
    List<CustomerView> findByTenantId(String tenantId);

    CustomerView save(CustomerView customer);

    Optional<CustomerView> findByTenantIdAndId(String tenantId, String id);

    boolean delete(String tenantId, String id);

    List<ContactView> findContacts(String tenantId, String customerId);

    ContactView saveContact(String tenantId, ContactView contact);

    List<FollowupView> findFollowups(String tenantId, String customerId);

    FollowupView saveFollowup(String tenantId, FollowupView followup);
}
