package com.vebcoding.trade.ai.service;

import com.vebcoding.trade.ai.api.KnowledgeReference;
import java.util.List;

public interface KnowledgeContextProvider {
    KnowledgeContext contextFor(String inquiryContent);

    record KnowledgeContext(String promptContent, List<KnowledgeReference> references) {
        public static KnowledgeContext empty() {
            return new KnowledgeContext("", List.of());
        }
    }
}
