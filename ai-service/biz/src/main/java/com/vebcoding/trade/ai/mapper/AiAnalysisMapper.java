package com.vebcoding.trade.ai.mapper;

import com.vebcoding.trade.ai.api.InquiryAnalysis;
import java.util.List;

public interface AiAnalysisMapper {
    InquiryAnalysis save(String inquiryId, InquiryAnalysis analysis);

    List<InquiryAnalysis> findByInquiryId(String tenantId, String inquiryId);
}
