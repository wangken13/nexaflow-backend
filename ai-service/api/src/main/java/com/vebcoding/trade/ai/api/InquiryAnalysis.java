package com.vebcoding.trade.ai.api;

import java.util.List;

public record InquiryAnalysis(String intent, String urgency, List<String> nextActions, String modelSummary,
                              String replyDraft, String quotationDraft, boolean knowledgeSufficient,
                              List<KnowledgeReference> sources) {
}
