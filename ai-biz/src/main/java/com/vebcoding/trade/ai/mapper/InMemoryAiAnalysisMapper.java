package com.vebcoding.trade.ai.mapper;

import com.vebcoding.trade.ai.api.InquiryAnalysis;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryAiAnalysisMapper implements AiAnalysisMapper {
    private final List<InquiryAnalysis> analyses = new CopyOnWriteArrayList<>();

    @Override
    public InquiryAnalysis save(InquiryAnalysis analysis) {
        analyses.add(analysis);
        return analysis;
    }
}