package com.vebcoding.trade.customer.service;

import com.vebcoding.trade.common.AccessDeniedException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.customer.mapper.CustomerMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CustomerAccessPolicy {
    private final CustomerMapper mapper;
    private final List<CustomerScopeStrategy> strategies;

    public CustomerAccessPolicy(CustomerMapper mapper, List<CustomerScopeStrategy> strategies) {
        this.mapper = mapper;
        this.strategies = strategies;
    }

    public CustomerAccessProfile currentProfile() {
        return mapper.findAccessProfile(TenantContext.tenantId(), TenantContext.userId(), TenantContext.role());
    }

    public List<CustomerView> visibleCustomers() {
        CustomerAccessProfile profile = currentProfile();
        return strategy(profile).findVisible(mapper, TenantContext.tenantId(), profile);
    }

    public void requireAccess(CustomerView customer) {
        CustomerAccessProfile profile = currentProfile();
        if (!strategy(profile).canAccess(customer, profile)) {
            throw new AccessDeniedException("当前账号无权查看或操作该客户");
        }
    }

    private CustomerScopeStrategy strategy(CustomerAccessProfile profile) {
        return strategies.stream().filter(item -> item.supports(profile.dataScope())).findFirst()
                .orElseThrow(() -> new AccessDeniedException("当前账号的数据权限配置无效，请联系企业管理员"));
    }
}
