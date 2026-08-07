package com.vebcoding.trade.tenant.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.tenant.api.TenantProfileResponse;
import com.vebcoding.trade.tenant.service.TenantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant")
public class TenantController {
    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/profile")
    public ApiResponse<TenantProfileResponse> profile() {
        return ApiResponse.ok(tenantService.profile());
    }
}