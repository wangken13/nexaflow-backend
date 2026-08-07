package com.vebcoding.trade.quotation.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.quotation.api.CreateQuotationRequest;
import com.vebcoding.trade.quotation.api.QuotationView;
import com.vebcoding.trade.quotation.mapper.QuotationMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class QuotationService {
    private final QuotationMapper quotationMapper;

    public QuotationService(QuotationMapper quotationMapper) {
        this.quotationMapper = quotationMapper;
    }

    public List<QuotationView> list() {
        return quotationMapper.findByTenantId(TenantContext.tenantId());
    }

    public QuotationView create(CreateQuotationRequest request) {
        QuotationView quotation = new QuotationView("quo-" + UUID.randomUUID(), TenantContext.tenantId(),
                request.customerId(), request.productName(), request.quantity(), request.unitPrice(), "DRAFT",
                Instant.now().toString());
        return quotationMapper.save(quotation);
    }
}