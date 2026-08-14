package com.vebcoding.trade.quotation.mapper;

import com.vebcoding.trade.quotation.api.QuotationView;
import com.vebcoding.trade.quotation.api.QuotationApprovalView;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryQuotationMapper implements QuotationMapper {
    private final List<QuotationView> quotations = new CopyOnWriteArrayList<>();
    private final List<QuotationApprovalView> approvals = new CopyOnWriteArrayList<>();

    @Override
    public List<QuotationView> findByTenantId(String tenantId) {
        return quotations.stream().filter(item -> tenantId.equals(item.tenantId())).toList();
    }

    @Override
    public QuotationView save(QuotationView quotation) {
        quotations.removeIf(item -> item.id().equals(quotation.id()));
        quotations.add(quotation);
        return quotation;
    }

    public Optional<QuotationView> findByTenantIdAndId(String tenantId, String id) {
        return quotations.stream().filter(item -> tenantId.equals(item.tenantId()) && id.equals(item.id())).findFirst();
    }

    @Override public List<QuotationApprovalView> findApprovals(String tenantId, String quotationId) { return approvals.stream().filter(item -> quotationId.equals(item.quotationId())).toList(); }
    @Override public QuotationApprovalView saveApproval(String tenantId, QuotationApprovalView approval) { approvals.add(approval); return approval; }
}
