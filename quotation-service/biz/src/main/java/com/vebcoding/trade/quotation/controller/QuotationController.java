package com.vebcoding.trade.quotation.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.quotation.api.CreateQuotationRequest;
import com.vebcoding.trade.quotation.api.QuotationView;
import com.vebcoding.trade.quotation.api.QuotationApprovalRequest;
import com.vebcoding.trade.quotation.api.QuotationApprovalView;
import jakarta.validation.Valid;
import com.vebcoding.trade.quotation.service.QuotationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quotation")
public class QuotationController {
    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @GetMapping
    public ApiResponse<List<QuotationView>> list() {
        return ApiResponse.ok(quotationService.list());
    }

    @PostMapping
    public ApiResponse<QuotationView> create(@RequestBody CreateQuotationRequest request) {
        return ApiResponse.ok(quotationService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<QuotationView> get(@PathVariable String id) {
        return ApiResponse.ok(quotationService.get(id));
    }

    @PatchMapping("/{id}/status/{status}")
    public ApiResponse<QuotationView> updateStatus(@PathVariable String id, @PathVariable String status) {
        return ApiResponse.ok(quotationService.updateStatus(id, status));
    }

    @GetMapping("/{id}/approvals")
    public ApiResponse<List<QuotationApprovalView>> approvals(@PathVariable String id) {
        return ApiResponse.ok(quotationService.approvals(id));
    }

    @PostMapping("/{id}/submit-approval")
    public ApiResponse<QuotationView> submitApproval(@PathVariable String id,
            @Valid @RequestBody QuotationApprovalRequest request) {
        return ApiResponse.ok(quotationService.submitApproval(id, request.comment()));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<QuotationView> approve(@PathVariable String id,
            @Valid @RequestBody QuotationApprovalRequest request) {
        return ApiResponse.ok(quotationService.decideApproval(id, true, request.comment()));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<QuotationView> reject(@PathVariable String id,
            @Valid @RequestBody QuotationApprovalRequest request) {
        return ApiResponse.ok(quotationService.decideApproval(id, false, request.comment()));
    }
}
