package com.vebcoding.trade.ai.api;

public record AiStreamEvent(String type, String delta, InquiryAnalysis analysis, String message) {
    public static AiStreamEvent delta(String content) {
        return new AiStreamEvent("delta", content, null, null);
    }

    public static AiStreamEvent completed(InquiryAnalysis analysis) {
        return new AiStreamEvent("complete", null, analysis, null);
    }

    public static AiStreamEvent failed(String message) {
        return new AiStreamEvent("error", null, null, message);
    }
}
