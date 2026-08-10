package com.vebcoding.trade.ai.service;

import org.springframework.stereotype.Component;

@Component
public class InquiryClassifier {
    public String detectIntent(String content) {
        String lower = content.toLowerCase();
        if (isNonTradeConsultation(content)) {
            return "非外贸业务咨询";
        }
        if (lower.contains("price") || lower.contains("quote") || content.contains("报价")) {
            return "报价询盘";
        }
        if (lower.contains("sample") || content.contains("样品")) {
            return "样品申请";
        }
        return "普通咨询";
    }

    public String detectUrgency(String content) {
        String lower = content.toLowerCase();
        return lower.contains("urgent") || content.contains("尽快") || content.contains("马上") ? "HIGH" : "NORMAL";
    }

    public boolean isNonTradeConsultation(String content) {
        String lower = content.toLowerCase();
        return lower.contains("java")
                || lower.contains("spring boot")
                || lower.contains("course")
                || content.contains("学习")
                || content.contains("课程")
                || content.contains("培训");
    }
}
