package com.vebcoding.trade.ai.mapper;

import com.vebcoding.trade.ai.api.InquiryAnalysis;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAiAnalysisMapper implements AiAnalysisMapper {
    private final List<InquiryAnalysis> analyses = new CopyOnWriteArrayList<>();
    private final Map<InquiryAnalysis, String> inquiryIds = new ConcurrentHashMap<>();

    @Override
    public InquiryAnalysis save(String inquiryId, InquiryAnalysis analysis) {
        analyses.add(analysis);
        inquiryIds.put(analysis, inquiryId);
        return analysis;
    }

    public List<InquiryAnalysis> findByInquiryId(String tenantId, String inquiryId) {
        return analyses.stream().filter(item -> inquiryId.equals(inquiryIds.get(item))).toList();
    }
}
