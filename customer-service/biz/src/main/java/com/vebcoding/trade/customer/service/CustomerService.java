package com.vebcoding.trade.customer.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.common.BulkImportResult;
import com.vebcoding.trade.common.ImportJobRecorder;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {
    private final CustomerMapper customerMapper;
    private final CustomerAccessPolicy accessPolicy;
    private final SensitiveDataMasker sensitiveDataMasker;
    private final ImportJobRecorder importJobRecorder;

    public CustomerService(CustomerMapper customerMapper, CustomerAccessPolicy accessPolicy,
                           SensitiveDataMasker sensitiveDataMasker, ImportJobRecorder importJobRecorder) {
        this.customerMapper = customerMapper;
        this.accessPolicy = accessPolicy;
        this.sensitiveDataMasker = sensitiveDataMasker;
        this.importJobRecorder = importJobRecorder;
    }

    public List<CustomerView> list() {
        return accessPolicy.visibleCustomers();
    }

    public List<CustomerView> exportData() {
        RoleGuard.requireAny("OWNER", "ADMIN");
        return customerMapper.findByTenantId(TenantContext.tenantId());
    }

    public CustomerView create(CreateCustomerRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        String name = TextSanitizer.required(request.name(), "客户名称");
        String country = TextSanitizer.optional(request.country());
        String tag = TextSanitizer.optional(request.tag());
        CustomerAccessProfile profile = accessPolicy.currentProfile();
        AssignableOwner owner = customerMapper.findAssignableOwner(TenantContext.tenantId(), profile.userId())
                .orElseThrow(() -> BusinessException.notFound("当前成员账号不存在或已停用"));
        CustomerView customer = new CustomerView("cus-" + UUID.randomUUID(), TenantContext.tenantId(),
                name, country, tag, owner.userId(), owner.displayName(), owner.departmentId(), owner.departmentName(),
                Instant.now().toString());
        return customerMapper.save(customer);
    }

    public CustomerDetailView detail(String id) {
        CustomerView customer = requireCustomer(id);
        CustomerDetailView detail = new CustomerDetailView(customer,
                customerMapper.findContacts(customer.tenantId(), id),
                customerMapper.findFollowups(customer.tenantId(), id));
        return sensitiveDataMasker.mask(detail, TenantContext.role());
    }

    public CustomerView update(String id, CreateCustomerRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        CustomerView current = requireCustomer(id);
        return customerMapper.save(new CustomerView(current.id(), current.tenantId(),
                TextSanitizer.required(request.name(), "客户名称"), TextSanitizer.optional(request.country()),
                TextSanitizer.optional(request.tag()), current.ownerId(), current.ownerName(), current.departmentId(),
                current.departmentName(), current.createdAt()));
    }

    public CustomerView assignOwner(String id, String ownerId) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        CustomerView current = requireCustomer(id);
        AssignableOwner owner = customerMapper.findAssignableOwner(TenantContext.tenantId(),
                        TextSanitizer.required(ownerId, "负责人"))
                .orElseThrow(() -> BusinessException.notFound("负责人不存在或已停用"));
        return customerMapper.save(new CustomerView(current.id(), current.tenantId(), current.name(),
                current.country(), current.tag(), owner.userId(), owner.displayName(), owner.departmentId(),
                owner.departmentName(), current.createdAt()));
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
        CustomerView customer = customerMapper.findByTenantIdAndId(TenantContext.tenantId(), id)
                .orElseThrow(() -> BusinessException.notFound("客户不存在"));
        accessPolicy.requireAccess(customer);
        return customer;
    }

    public List<String> tags() {
        return List.of("new", "vip", "at-risk", "quoted");
    }

    public BulkImportResult bulkImport(List<CreateCustomerRequest> rows) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        if (rows == null || rows.isEmpty() || rows.size() > 500) throw new BusinessException("单次导入数量必须为1至500条");
        String jobId = importJobRecorder.start("CUSTOMER", rows.size());
        HashSet<String> names = customerMapper.findByTenantId(TenantContext.tenantId()).stream()
                .map(item -> item.name().trim().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        List<String> errors = new ArrayList<>();
        int imported = 0;
        for (int index = 0; index < rows.size(); index++) {
            CreateCustomerRequest row = rows.get(index);
            String name = TextSanitizer.optional(row.name());
            if (name.isBlank()) { errors.add("第" + (index + 1) + "行：客户名称不能为空"); continue; }
            if (!names.add(name.toLowerCase(Locale.ROOT))) { errors.add("第" + (index + 1) + "行：客户名称重复"); continue; }
            create(row);
            imported++;
        }
        return importJobRecorder.complete(jobId, rows.size(), imported, errors);
    }
}
