package com.vebcoding.trade.ai;

import com.vebcoding.trade.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiController {
    private final InquiryAnalysisService inquiryAnalysisService;

    public AiController(InquiryAnalysisService inquiryAnalysisService) {
        this.inquiryAnalysisService = inquiryAnalysisService;
    }

    @PostMapping("/analyze-inquiry")
    public ApiResponse<InquiryAnalysis> analyzeInquiry(@RequestBody AnalyzeInquiryRequest request) {
        return ApiResponse.ok(inquiryAnalysisService.analyze(request.content()));
    }

    public record AnalyzeInquiryRequest(@NotBlank String content) {
    }

    public record InquiryAnalysis(String intent, String urgency, List<String> nextActions, String analysisText,
                                  String replyDraft, String quotationDraft) {
    }
}
