package com.vebcoding.trade.ai.service;

import com.vebcoding.trade.ai.api.InquiryAnalysis;
import com.vebcoding.trade.ai.mapper.AiAnalysisMapper;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.RoleGuard;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiService {
    private final AiProviderStrategy aiProviderStrategy;
    private final InquiryClassifier inquiryClassifier;
    private final InquiryDraftFactory inquiryDraftFactory;
    private final AiAnalysisMapper aiAnalysisMapper;

    public AiService(AiProviderStrategy aiProviderStrategy, InquiryClassifier inquiryClassifier,
                     InquiryDraftFactory inquiryDraftFactory, AiAnalysisMapper aiAnalysisMapper) {
        this.aiProviderStrategy = aiProviderStrategy;
        this.inquiryClassifier = inquiryClassifier;
        this.inquiryDraftFactory = inquiryDraftFactory;
        this.aiAnalysisMapper = aiAnalysisMapper;
    }

    public InquiryAnalysis analyzeInquiry(String content) {
        return analyzeInquiry("", content);
    }

    public InquiryAnalysis analyzeInquiry(String inquiryId, String content) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        String intent = inquiryClassifier.detectIntent(content);
        String urgency = inquiryClassifier.detectUrgency(content);
        String prompt = """
                你是企业级外贸跟单助手。请分析客户原文，输出一段纯中文文本，不要 Markdown，不要加粗符号。
                分析必须严格贴合客户真实要求：如果不是外贸产品询盘，不要生成产品报价；如果信息不足，要说明缺少哪些信息。
                需要覆盖：客户需求、关键缺口、建议回应策略。
                客户原文：
                %s
                """.formatted(content);
        String modelSummary = inquiryDraftFactory.requirementSummary(
                content,
                aiProviderStrategy.generate(prompt),
                intent,
                urgency);
        InquiryAnalysis analysis = new InquiryAnalysis(
                intent,
                urgency,
                inquiryDraftFactory.nextActions(content, intent),
                modelSummary,
                inquiryDraftFactory.replyDraft(content, intent),
                inquiryDraftFactory.quotationDraft(content, intent));
        return aiAnalysisMapper.save(inquiryId, analysis);
    }

    public List<InquiryAnalysis> history(String inquiryId) {
        return aiAnalysisMapper.findByInquiryId(TenantContext.tenantId(), inquiryId);
    }
}
