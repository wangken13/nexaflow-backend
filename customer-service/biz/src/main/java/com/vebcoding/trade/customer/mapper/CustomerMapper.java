package com.vebcoding.trade.customer.mapper;

import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.customer.api.ContactView;
import com.vebcoding.trade.customer.api.FollowupView;
import com.vebcoding.trade.customer.service.AssignableOwner;
import com.vebcoding.trade.customer.service.CustomerAccessProfile;
import java.util.List;
import java.util.Optional;

public interface CustomerMapper {
    List<CustomerView> findByTenantId(String tenantId);

    List<CustomerView> findByTenantIdAndOwnerId(String tenantId, String ownerId);

    List<CustomerView> findByTenantIdAndDepartmentId(String tenantId, String departmentId);

    CustomerAccessProfile findAccessProfile(String tenantId, String userId, String fallbackRole);

    Optional<AssignableOwner> findAssignableOwner(String tenantId, String userId);

    CustomerView save(CustomerView customer);

    Optional<CustomerView> findByTenantIdAndId(String tenantId, String id);

    boolean delete(String tenantId, String id);

    List<ContactView> findContacts(String tenantId, String customerId);

    ContactView saveContact(String tenantId, ContactView contact);

    List<FollowupView> findFollowups(String tenantId, String customerId);

    FollowupView saveFollowup(String tenantId, FollowupView followup);
}
