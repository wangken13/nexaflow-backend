package com.vebcoding.trade.tenant.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.tenant.api.TenantProfileResponse;
import com.vebcoding.trade.tenant.api.AuditLogView;
import com.vebcoding.trade.tenant.api.CreateMemberRequest;
import com.vebcoding.trade.tenant.api.MemberView;
import com.vebcoding.trade.tenant.service.TenantService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/members")
    public ApiResponse<List<MemberView>> members() {
        return ApiResponse.ok(tenantService.members());
    }

    @PostMapping("/members")
    public ApiResponse<MemberView> createMember(@Valid @RequestBody CreateMemberRequest request) {
        return ApiResponse.ok(tenantService.createMember(request));
    }

    @PatchMapping("/members/{id}/role/{role}")
    public ApiResponse<MemberView> updateMemberRole(@PathVariable String id, @PathVariable String role) {
        return ApiResponse.ok(tenantService.updateMemberRole(id, role));
    }

    @PatchMapping("/members/{id}/status/{status}")
    public ApiResponse<MemberView> updateMemberStatus(@PathVariable String id, @PathVariable String status) {
        return ApiResponse.ok(tenantService.updateMemberStatus(id, status));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<AuditLogView>> auditLogs(
            @RequestParam(defaultValue = "") String module,
            @RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.ok(tenantService.auditLogs(module, keyword));
    }
}
