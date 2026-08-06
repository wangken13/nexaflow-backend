package com.vebcoding.trade.ai;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AiAnalysisListener {
    private static final Logger log = LoggerFactory.getLogger(AiAnalysisListener.class);

    @RabbitListener(queues = "trade.ai.analysis.requests")
    public void onAnalysisRequested(Map<String, String> event) {
        log.info("Received AI analysis request inquiryId={} tenantId={}", event.get("inquiryId"), event.get("tenantId"));
    }
}
