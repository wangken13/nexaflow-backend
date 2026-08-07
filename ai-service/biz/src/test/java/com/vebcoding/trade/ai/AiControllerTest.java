package com.vebcoding.trade.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.ai.api.AnalyzeInquiryRequest;
import com.vebcoding.trade.ai.api.InquiryAnalysis;
import com.vebcoding.trade.ai.controller.AiController;
import com.vebcoding.trade.ai.mapper.InMemoryAiAnalysisMapper;
import com.vebcoding.trade.ai.service.AiProviderStrategy;
import com.vebcoding.trade.ai.service.AiService;
import com.vebcoding.trade.ai.service.InquiryClassifier;
import com.vebcoding.trade.ai.service.InquiryDraftFactory;
import com.vebcoding.trade.common.ApiResponse;
import org.junit.jupiter.api.Test;

class AiControllerTest {
    @Test
    void analyzeInquiryDetectsQuoteIntent() {
        AiProviderStrategy provider = prompt -> "本地分析";
        AiService service = new AiService(provider, new InquiryClassifier(), new InquiryDraftFactory(),
                new InMemoryAiAnalysisMapper());
        AiController controller = new AiController(service);

        ApiResponse<InquiryAnalysis> response = controller.analyzeInquiry(new AnalyzeInquiryRequest("Please quote 500 pcs"));

        assertThat(response.data().intent()).isEqualTo("报价询盘");
        assertThat(response.data().nextActions()).isNotEmpty();
    }
}
