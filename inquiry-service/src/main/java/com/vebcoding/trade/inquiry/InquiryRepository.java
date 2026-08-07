package com.vebcoding.trade.inquiry;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository {
    List<InquiryController.InquiryView> findByTenantId(String tenantId);

    InquiryController.InquiryView save(InquiryController.InquiryView inquiry);

    Optional<InquiryController.InquiryView> findByTenantIdAndId(String tenantId, String id);
}
