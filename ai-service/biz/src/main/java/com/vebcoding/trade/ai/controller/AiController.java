package com.vebcoding.trade.ai.controller;

import com.vebcoding.trade.ai.api.AnalyzeInquiryRequest;
import com.vebcoding.trade.ai.api.InquiryAnalysis;
import com.vebcoding.trade.ai.service.AiService;
import com.vebcoding.trade.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiController {
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/analyze-inquiry")
    public ApiResponse<InquiryAnalysis> analyzeInquiry(@RequestBody AnalyzeInquiryRequest request) {
        return ApiResponse.ok(aiService.analyzeInquiry(request.content()));
    }
}