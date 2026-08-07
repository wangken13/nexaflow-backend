package com.vebcoding.trade.tenant.mapper;

import com.vebcoding.trade.tenant.api.TenantProfileResponse;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTenantMapper implements TenantMapper {
    @Override
    public TenantProfileResponse findProfile(String tenantId) {
        return new TenantProfileResponse(tenantId, "Demo 澶栬锤鍥㈤槦", "PRO", 128, 3000);
    }
}