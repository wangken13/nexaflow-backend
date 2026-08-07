package com.vebcoding.trade.ai.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AiAnalysisListener {
    private final AiService aiService;

    public AiAnalysisListener(AiService aiService) {
        this.aiService = aiService;
    }

    @RabbitListener(queues = "trade.ai.analysis")
    public void onAnalysisRequested(String content) {
        aiService.analyzeInquiry(content);
    }
}