package com.vebcoding.trade.quotation.mapper;

import com.vebcoding.trade.quotation.api.QuotationView;
import com.vebcoding.trade.quotation.api.QuotationApprovalView;
import java.util.List;
import java.util.Optional;

public interface QuotationMapper {
    List<QuotationView> findByTenantId(String tenantId);

    QuotationView save(QuotationView quotation);

    Optional<QuotationView> findByTenantIdAndId(String tenantId, String id);

    List<QuotationApprovalView> findApprovals(String tenantId, String quotationId);

    QuotationApprovalView saveApproval(String tenantId, QuotationApprovalView approval);
}
