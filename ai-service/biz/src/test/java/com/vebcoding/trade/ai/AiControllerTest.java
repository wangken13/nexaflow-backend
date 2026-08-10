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
import com.vebcoding.trade.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiControllerTest {
    @BeforeEach
    void authenticate() {
        TenantContext.setTenantId("demo-tenant");
        TenantContext.setUserId("admin");
        TenantContext.setRole("OWNER");
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void analyzeInquiryDetectsQuoteIntent() {
        AiProviderStrategy provider = prompt -> "本地分析";
        AiService service = new AiService(provider, new InquiryClassifier(), new InquiryDraftFactory(),
                new InMemoryAiAnalysisMapper());
        AiController controller = new AiController(service);

        ApiResponse<InquiryAnalysis> response = controller.analyzeInquiry(new AnalyzeInquiryRequest("Please quote 500 pcs"));

        assertThat(response.data().intent()).isEqualTo("报价询盘");
        assertThat(response.data().nextActions()).isNotEmpty();
        assertThat(response.data().quotationDraft()).contains("500 pcs");
    }

    @Test
    void analyzeInquiryDoesNotCreateProductQuoteForNonTradeConsultation() {
        AiProviderStrategy provider = prompt -> "**销售建议：** 客户想学习 Java";
        AiService service = new AiService(provider, new InquiryClassifier(), new InquiryDraftFactory(),
                new InMemoryAiAnalysisMapper());
        AiController controller = new AiController(service);

        ApiResponse<InquiryAnalysis> response = controller.analyzeInquiry(
                new AnalyzeInquiryRequest("我想学习Java，应该怎么开始？"));

        assertThat(response.data().intent()).isEqualTo("非外贸业务咨询");
        assertThat(response.data().modelSummary()).doesNotContain("**");
        assertThat(response.data().quotationDraft()).contains("暂不生成");
        assertThat(response.data().nextActions()).contains("确认客户真实需求");
    }
}
