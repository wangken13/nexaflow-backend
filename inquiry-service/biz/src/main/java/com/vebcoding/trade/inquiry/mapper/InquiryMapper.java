package com.vebcoding.trade.inquiry.mapper;

import com.vebcoding.trade.inquiry.api.InquiryView;
import java.util.List;
import java.util.Optional;

public interface InquiryMapper {
    List<InquiryView> findByTenantId(String tenantId);

    InquiryView save(InquiryView inquiry);

    Optional<InquiryView> findByTenantIdAndId(String tenantId, String id);
}