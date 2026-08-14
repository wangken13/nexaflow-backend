package com.vebcoding.trade.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.vebcoding.trade.ai.api.AnalyzeInquiryRequest;
import com.vebcoding.trade.ai.api.AiProviderStatus;
import com.vebcoding.trade.ai.api.InquiryAnalysis;
import com.vebcoding.trade.ai.api.KnowledgeReference;
import com.vebcoding.trade.ai.controller.AiController;
import com.vebcoding.trade.ai.mapper.InMemoryAiAnalysisMapper;
import com.vebcoding.trade.ai.service.AiProviderStrategy;
import com.vebcoding.trade.ai.service.AiService;
import com.vebcoding.trade.ai.service.AiStreamingService;
import com.vebcoding.trade.ai.service.InquiryClassifier;
import com.vebcoding.trade.ai.service.InquiryDraftFactory;
import com.vebcoding.trade.ai.service.KnowledgeContextProvider;
import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;

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
        AiController controller = new AiController(service, mock(AiStreamingService.class));

        ApiResponse<InquiryAnalysis> response = controller.analyzeInquiry(new AnalyzeInquiryRequest("Please quote 500 pcs"));

        assertThat(response.data().intent()).isEqualTo("报价询盘");
        assertThat(response.data().nextActions()).isNotEmpty();
        assertThat(response.data().knowledgeSufficient()).isFalse();
        assertThat(response.data().quotationDraft()).contains("企业知识库暂无可引用");
    }

    @Test
    void analyzeInquiryDoesNotCreateProductQuoteForNonTradeConsultation() {
        AiProviderStrategy provider = prompt -> "**销售建议：** 客户想学习 Java";
        AiService service = new AiService(provider, new InquiryClassifier(), new InquiryDraftFactory(),
                new InMemoryAiAnalysisMapper());
        AiController controller = new AiController(service, mock(AiStreamingService.class));

        ApiResponse<InquiryAnalysis> response = controller.analyzeInquiry(
                new AnalyzeInquiryRequest("我想学习Java，应该怎么开始？"));

        assertThat(response.data().intent()).isEqualTo("非外贸业务咨询");
        assertThat(response.data().modelSummary()).doesNotContain("**");
        assertThat(response.data().quotationDraft()).contains("暂不生成");
        assertThat(response.data().nextActions()).contains("确认客户真实需求");
    }

    @Test
    void providerStatusIsAvailableToTenantAdministrators() {
        AiProviderStrategy provider = new AiProviderStrategy() {
            @Override
            public String generate(String prompt) {
                return "本地分析";
            }

            @Override
            public AiProviderStatus status() {
                return new AiProviderStatus("DeepSeek", "deepseek-chat", true, true);
            }
        };
        AiService service = new AiService(provider, new InquiryClassifier(), new InquiryDraftFactory(),
                new InMemoryAiAnalysisMapper());

        ApiResponse<AiProviderStatus> response = new AiController(service, mock(AiStreamingService.class)).providerStatus();

        assertThat(response.data().provider()).isEqualTo("DeepSeek");
        assertThat(response.data().configured()).isTrue();
        assertThat(response.data().fallbackEnabled()).isTrue();
    }

    @Test
    void analysisPromptIncludesTenantKnowledgeContext() {
        AtomicReference<String> prompt = new AtomicReference<>();
        AiProviderStrategy provider = value -> { prompt.set(value); return "模型分析"; };
        AiService service = new AiService(provider, new InquiryClassifier(), new InquiryDraftFactory(),
                new InMemoryAiAnalysisMapper(), content -> new KnowledgeContextProvider.KnowledgeContext(
                "[KB:kb-1][DELIVERY] 标准交期\n常规产品30天交付",
                java.util.List.of(new KnowledgeReference("kb-1", "标准交期", "DELIVERY"))));

        InquiryAnalysis analysis = new AiController(service, mock(AiStreamingService.class))
                .analyzeInquiry(new AnalyzeInquiryRequest("Please confirm lead time")).data();

        assertThat(prompt.get()).contains("企业知识库", "常规产品30天交付", "Please confirm lead time");
        assertThat(analysis.knowledgeSufficient()).isTrue();
        assertThat(analysis.sources()).extracting("id").containsExactly("kb-1");
    }
}
