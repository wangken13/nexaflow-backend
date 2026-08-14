package com.vebcoding.trade.tenant.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.tenant.api.TenantProfileResponse;
import com.vebcoding.trade.tenant.api.AuditLogView;
import com.vebcoding.trade.tenant.api.CreateMemberRequest;
import com.vebcoding.trade.tenant.api.MemberView;
import com.vebcoding.trade.tenant.api.ChannelConfigView;
import com.vebcoding.trade.tenant.api.KnowledgeArticleView;
import com.vebcoding.trade.tenant.api.SubscriptionView;
import com.vebcoding.trade.tenant.api.UpsertChannelConfigRequest;
import com.vebcoding.trade.tenant.api.UpsertKnowledgeArticleRequest;
import com.vebcoding.trade.tenant.api.CreateDepartmentRequest;
import com.vebcoding.trade.tenant.api.DepartmentView;
import com.vebcoding.trade.tenant.api.UpdateMemberAccessRequest;
import com.vebcoding.trade.tenant.api.ImportJobView;
import com.vebcoding.trade.tenant.api.CreateInvoiceRequest;
import com.vebcoding.trade.tenant.api.CreateRefundRequest;
import com.vebcoding.trade.tenant.api.CreateSubscriptionOrderRequest;
import com.vebcoding.trade.tenant.api.InvoiceRequestView;
import com.vebcoding.trade.tenant.api.PlanView;
import com.vebcoding.trade.tenant.api.RefundRequestView;
import com.vebcoding.trade.tenant.api.SubscriptionOrderView;
import com.vebcoding.trade.tenant.service.BillingService;
import com.vebcoding.trade.tenant.service.TenantService;
import com.vebcoding.trade.tenant.service.OnboardingService;
import com.vebcoding.trade.tenant.api.OnboardingView;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/tenant")
public class TenantController {
    private final TenantService tenantService;
    private final BillingService billingService;
    private final OnboardingService onboardingService;

    public TenantController(TenantService tenantService, BillingService billingService) {
        this(tenantService, billingService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TenantController(TenantService tenantService, BillingService billingService,
                            OnboardingService onboardingService) {
        this.tenantService = tenantService;
        this.billingService = billingService;
        this.onboardingService = onboardingService;
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

    @PatchMapping("/members/{id}/access")
    public ApiResponse<MemberView> updateMemberAccess(@PathVariable String id,
                                                       @Valid @RequestBody UpdateMemberAccessRequest request) {
        return ApiResponse.ok(tenantService.updateMemberAccess(id, request));
    }

    @GetMapping("/departments")
    public ApiResponse<List<DepartmentView>> departments() {
        return ApiResponse.ok(tenantService.departments());
    }

    @PostMapping("/departments")
    public ApiResponse<DepartmentView> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return ApiResponse.ok(tenantService.createDepartment(request));
    }

    @PatchMapping("/departments/{id}/status/{status}")
    public ApiResponse<DepartmentView> updateDepartmentStatus(@PathVariable String id, @PathVariable String status) {
        return ApiResponse.ok(tenantService.updateDepartmentStatus(id, status));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<AuditLogView>> auditLogs(
            @RequestParam(defaultValue = "") String module,
            @RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.ok(tenantService.auditLogs(module, keyword));
    }

    @GetMapping("/import-jobs")
    public ApiResponse<List<ImportJobView>> importJobs() {
        return ApiResponse.ok(tenantService.importJobs());
    }

    @GetMapping("/knowledge")
    public ApiResponse<List<KnowledgeArticleView>> knowledgeArticles() {
        return ApiResponse.ok(tenantService.knowledgeArticles());
    }

    @PostMapping("/knowledge")
    public ApiResponse<KnowledgeArticleView> createKnowledgeArticle(
            @Valid @RequestBody UpsertKnowledgeArticleRequest request) {
        return ApiResponse.ok(tenantService.createKnowledgeArticle(request));
    }

    @PutMapping("/knowledge/{id}")
    public ApiResponse<KnowledgeArticleView> updateKnowledgeArticle(
            @PathVariable String id, @Valid @RequestBody UpsertKnowledgeArticleRequest request) {
        return ApiResponse.ok(tenantService.updateKnowledgeArticle(id, request));
    }

    @DeleteMapping("/knowledge/{id}")
    public ApiResponse<Void> deleteKnowledgeArticle(@PathVariable String id) {
        tenantService.deleteKnowledgeArticle(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/channels")
    public ApiResponse<List<ChannelConfigView>> channels() {
        return ApiResponse.ok(tenantService.channels());
    }

    @PutMapping("/channels")
    public ApiResponse<ChannelConfigView> saveChannel(@Valid @RequestBody UpsertChannelConfigRequest request) {
        return ApiResponse.ok(tenantService.saveChannel(request));
    }

    @GetMapping("/subscription")
    public ApiResponse<SubscriptionView> subscription() {
        return ApiResponse.ok(tenantService.subscription());
    }

    @GetMapping("/onboarding")
    public ApiResponse<OnboardingView> onboarding() { return ApiResponse.ok(onboardingService.status()); }

    @PostMapping("/onboarding/demo-data")
    public ApiResponse<OnboardingView> createDemoData() { return ApiResponse.ok(onboardingService.createDemoData()); }

    @DeleteMapping("/onboarding/demo-data")
    public ApiResponse<OnboardingView> clearDemoData() { return ApiResponse.ok(onboardingService.clearDemoData()); }

    @GetMapping("/billing/plans")
    public ApiResponse<List<PlanView>> plans() { return ApiResponse.ok(billingService.plans()); }

    @GetMapping("/billing/orders")
    public ApiResponse<List<SubscriptionOrderView>> billingOrders() { return ApiResponse.ok(billingService.orders()); }

    @PostMapping("/billing/orders")
    public ApiResponse<SubscriptionOrderView> createBillingOrder(
            @Valid @RequestBody CreateSubscriptionOrderRequest request) {
        return ApiResponse.ok(billingService.createOrder(request));
    }

    @GetMapping("/billing/invoices")
    public ApiResponse<List<InvoiceRequestView>> invoices() { return ApiResponse.ok(billingService.invoices()); }

    @PostMapping("/billing/orders/{id}/invoice")
    public ApiResponse<InvoiceRequestView> createInvoice(@PathVariable String id,
                                                          @Valid @RequestBody CreateInvoiceRequest request) {
        return ApiResponse.ok(billingService.createInvoice(id, request));
    }

    @GetMapping("/billing/refunds")
    public ApiResponse<List<RefundRequestView>> refunds() { return ApiResponse.ok(billingService.refunds()); }

    @PostMapping("/billing/orders/{id}/refund")
    public ApiResponse<RefundRequestView> createRefund(@PathVariable String id,
                                                        @Valid @RequestBody CreateRefundRequest request) {
        return ApiResponse.ok(billingService.createRefund(id, request));
    }

    @PostMapping("/billing/callback")
    public ApiResponse<SubscriptionOrderView> billingCallback(
            @org.springframework.web.bind.annotation.RequestHeader("X-Nexa-Timestamp") String timestamp,
            @org.springframework.web.bind.annotation.RequestHeader("X-Nexa-Signature") String signature,
            @RequestBody String body) {
        return ApiResponse.ok(billingService.handleCallback(timestamp, signature, body));
    }
}
