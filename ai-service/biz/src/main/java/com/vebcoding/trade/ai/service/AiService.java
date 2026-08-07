package com.vebcoding.trade.ai.service;

import com.vebcoding.trade.ai.api.InquiryAnalysis;
import com.vebcoding.trade.ai.mapper.AiAnalysisMapper;
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
        String prompt = """
                You are an export sales assistant. Analyze this inquiry and return concise Chinese sales advice:
                %s
                """.formatted(content);
        InquiryAnalysis analysis = new InquiryAnalysis(
                inquiryClassifier.detectIntent(content),
                inquiryClassifier.detectUrgency(content),
                inquiryDraftFactory.nextActions(),
                aiProviderStrategy.generate(prompt),
                inquiryDraftFactory.replyDraft(),
                inquiryDraftFactory.quotationDraft());
        return aiAnalysisMapper.save(analysis);
    }
}