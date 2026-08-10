package com.vebcoding.trade.customer.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.customer.api.CreateCustomerRequest;
import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.customer.api.ContactView;
import com.vebcoding.trade.customer.api.CreateContactRequest;
import com.vebcoding.trade.customer.api.CreateFollowupRequest;
import com.vebcoding.trade.customer.api.CustomerDetailView;
import com.vebcoding.trade.customer.api.FollowupView;
import com.vebcoding.trade.customer.mapper.CustomerMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    public List<CustomerView> list() {
        return customerMapper.findByTenantId(TenantContext.tenantId());
    }

    public CustomerView create(CreateCustomerRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        String name = TextSanitizer.required(request.name(), "客户名称");
        String country = TextSanitizer.optional(request.country());
        String tag = TextSanitizer.optional(request.tag());
        CustomerView customer = new CustomerView("cus-" + UUID.randomUUID(), TenantContext.tenantId(),
                name, country, tag, Instant.now().toString());
        return customerMapper.save(customer);
    }

    public CustomerDetailView detail(String id) {
        CustomerView customer = requireCustomer(id);
        return new CustomerDetailView(customer, customerMapper.findContacts(customer.tenantId(), id),
                customerMapper.findFollowups(customer.tenantId(), id));
    }

    public CustomerView update(String id, CreateCustomerRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        CustomerView current = requireCustomer(id);
        return customerMapper.save(new CustomerView(current.id(), current.tenantId(),
                TextSanitizer.required(request.name(), "客户名称"), TextSanitizer.optional(request.country()),
                TextSanitizer.optional(request.tag()), current.createdAt()));
    }

    public void delete(String id) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        if (!customerMapper.delete(TenantContext.tenantId(), id)) throw BusinessException.notFound("客户不存在");
    }

    public ContactView addContact(String customerId, CreateContactRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        requireCustomer(customerId);
        ContactView contact = new ContactView("con-" + UUID.randomUUID(), customerId,
                TextSanitizer.required(request.name(), "联系人姓名"), TextSanitizer.optional(request.email()),
                TextSanitizer.optional(request.phone()), TextSanitizer.optional(request.position()), request.primary(),
                Instant.now().toString());
        return customerMapper.saveContact(TenantContext.tenantId(), contact);
    }

    public FollowupView addFollowup(String customerId, CreateFollowupRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        requireCustomer(customerId);
        String type = TextSanitizer.optional(request.type()).toUpperCase();
        if (type.isBlank()) type = "NOTE";
        FollowupView followup = new FollowupView("fup-" + UUID.randomUUID(), customerId, type,
                TextSanitizer.required(request.content(), "跟进内容"), TextSanitizer.optional(request.operatorName()),
                Instant.now().toString());
        return customerMapper.saveFollowup(TenantContext.tenantId(), followup);
    }

    private CustomerView requireCustomer(String id) {
        return customerMapper.findByTenantIdAndId(TenantContext.tenantId(), id)
                .orElseThrow(() -> BusinessException.notFound("客户不存在"));
    }

    public List<String> tags() {
        return List.of("new", "vip", "at-risk", "quoted");
    }
}
