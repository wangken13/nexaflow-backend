package com.vebcoding.trade.tenant.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.tenant.api.TenantProfileResponse;
import com.vebcoding.trade.tenant.mapper.TenantMapper;
import org.springframework.stereotype.Service;

@Service
public class TenantService {
    private final TenantMapper tenantMapper;

    public TenantService(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    public TenantProfileResponse profile() {
        return tenantMapper.findProfile(TenantContext.tenantId());
    }
}