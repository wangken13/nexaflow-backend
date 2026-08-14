package com.vebcoding.trade.ai.controller;

import com.vebcoding.trade.ai.api.AnalyzeInquiryRequest;
import com.vebcoding.trade.ai.api.AiProviderStatus;
import com.vebcoding.trade.ai.api.InquiryAnalysis;
import com.vebcoding.trade.ai.service.AiService;
import com.vebcoding.trade.ai.service.AiStreamingService;
import com.vebcoding.trade.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai")
public class AiController {
    private final AiService aiService;
    private final AiStreamingService aiStreamingService;

    public AiController(AiService aiService, AiStreamingService aiStreamingService) {
        this.aiService = aiService;
        this.aiStreamingService = aiStreamingService;
    }

    @PostMapping("/analyze-inquiry")
    public ApiResponse<InquiryAnalysis> analyzeInquiry(@Valid @RequestBody AnalyzeInquiryRequest request) {
        return ApiResponse.ok(aiService.analyzeInquiry(request.inquiryId(), request.content()));
    }

    @PostMapping(value = "/analyze-inquiry/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeInquiryStream(@Valid @RequestBody AnalyzeInquiryRequest request) {
        return aiStreamingService.analyzeInquiry(request.inquiryId(), request.content());
    }

    @GetMapping("/inquiries/{inquiryId}/history")
    public ApiResponse<List<InquiryAnalysis>> history(@PathVariable String inquiryId) {
        return ApiResponse.ok(aiService.history(inquiryId));
    }

    @GetMapping("/provider-status")
    public ApiResponse<AiProviderStatus> providerStatus() {
        return ApiResponse.ok(aiService.providerStatus());
    }
}
