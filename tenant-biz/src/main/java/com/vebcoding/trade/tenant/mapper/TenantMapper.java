package com.vebcoding.trade.tenant.mapper;

import com.vebcoding.trade.tenant.api.TenantProfileResponse;

public interface TenantMapper {
    TenantProfileResponse findProfile(String tenantId);
}