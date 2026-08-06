package com.vebcoding.trade.ai;

import com.vebcoding.trade.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiController {
    private final ObjectProvider<ChatClient.Builder> chatClientBuilder;

    public AiController(ObjectProvider<ChatClient.Builder> chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostMapping("/analyze-inquiry")
    public ApiResponse<InquiryAnalysis> analyzeInquiry(@RequestBody AnalyzeInquiryRequest request) {
        String prompt = """
                You are an export sales assistant. Analyze this inquiry and return concise Chinese sales advice:
                %s
                """.formatted(request.content());
        String modelText = callModel(prompt);
        InquiryAnalysis analysis = new InquiryAnalysis(
                detectIntent(request.content()),
                detectUrgency(request.content()),
                List.of("确认产品规格", "补充 MOQ 和交期", "生成报价草稿"),
                modelText,
                "您好，感谢您的询盘。我们可以根据您的数量和目标交期提供报价，请确认规格、包装和目的港。",
                "建议报价：FOB Shanghai，单价按数量阶梯计算，有效期 7 天。");
        return ApiResponse.ok(analysis);
    }

    private String callModel(String prompt) {
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (builder == null) {
            return "未配置模型 Key，已使用本地规则生成分析。";
        }
        try {
            return builder.build().prompt(prompt).call().content();
        } catch (Exception ex) {
            return "AI 调用失败，已降级为本地规则分析：" + ex.getMessage();
        }
    }

    private String detectIntent(String content) {
        String lower = content.toLowerCase();
        if (lower.contains("price") || lower.contains("quote") || content.contains("报价")) {
            return "报价询盘";
        }
        if (lower.contains("sample") || content.contains("样品")) {
            return "样品申请";
        }
        return "普通咨询";
    }

    private String detectUrgency(String content) {
        String lower = content.toLowerCase();
        return lower.contains("urgent") || content.contains("尽快") || content.contains("马上") ? "HIGH" : "NORMAL";
    }

    public record AnalyzeInquiryRequest(@NotBlank String content) {
    }

    public record InquiryAnalysis(String intent, String urgency, List<String> nextActions, String analysisText,
                                  String replyDraft, String quotationDraft) {
    }
}
