package com.vebcoding.trade.ai;

import org.springframework.stereotype.Service;

@Service
public class InquiryAnalysisService {
    private final AiProviderStrategy aiProviderStrategy;
    private final InquiryClassifier inquiryClassifier;
    private final InquiryDraftFactory inquiryDraftFactory;

    public InquiryAnalysisService(AiProviderStrategy aiProviderStrategy, InquiryClassifier inquiryClassifier,
                                  InquiryDraftFactory inquiryDraftFactory) {
        this.aiProviderStrategy = aiProviderStrategy;
        this.inquiryClassifier = inquiryClassifier;
        this.inquiryDraftFactory = inquiryDraftFactory;
    }

    public AiController.InquiryAnalysis analyze(String content) {
        String prompt = """
                You are an export sales assistant. Analyze this inquiry and return concise Chinese sales advice:
                %s
                """.formatted(content);
        return new AiController.InquiryAnalysis(
                inquiryClassifier.detectIntent(content),
                inquiryClassifier.detectUrgency(content),
                inquiryDraftFactory.nextActions(),
                aiProviderStrategy.generate(prompt),
                inquiryDraftFactory.replyDraft(),
                inquiryDraftFactory.quotationDraft());
    }
}
