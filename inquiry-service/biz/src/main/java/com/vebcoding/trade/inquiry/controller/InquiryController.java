package com.vebcoding.trade.inquiry.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.inquiry.api.CreateInquiryRequest;
import com.vebcoding.trade.inquiry.api.InquiryView;
import com.vebcoding.trade.inquiry.service.InquiryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inquiry")
public class InquiryController {
    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping
    public ApiResponse<List<InquiryView>> list() {
        return ApiResponse.ok(inquiryService.list());
    }

    @PostMapping
    public ApiResponse<InquiryView> create(@RequestBody CreateInquiryRequest request) {
        return ApiResponse.ok(inquiryService.create(request));
    }

    @PatchMapping("/{id}/status/{status}")
    public ApiResponse<InquiryView> updateStatus(@PathVariable String id, @PathVariable String status) {
        return inquiryService.updateStatus(id, status)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("询盘不存在"));
    }
}
