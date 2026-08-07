package com.vebcoding.trade.quotation.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.quotation.api.CreateQuotationRequest;
import com.vebcoding.trade.quotation.api.QuotationView;
import com.vebcoding.trade.quotation.service.QuotationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
}