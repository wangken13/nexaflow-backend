package com.vebcoding.trade.quotation.mapper;

import com.vebcoding.trade.quotation.api.QuotationView;
import java.util.List;

public interface QuotationMapper {
    List<QuotationView> findByTenantId(String tenantId);

    QuotationView save(QuotationView quotation);
}