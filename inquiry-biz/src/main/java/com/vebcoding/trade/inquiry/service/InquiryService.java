package com.vebcoding.trade.inquiry.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.inquiry.api.CreateInquiryRequest;
import com.vebcoding.trade.inquiry.api.InquiryView;
import com.vebcoding.trade.inquiry.mapper.InquiryMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InquiryService {
    private final InquiryMapper inquiryMapper;
    private final InquiryEventPublisher inquiryEventPublisher;

    public InquiryService(InquiryMapper inquiryMapper, InquiryEventPublisher inquiryEventPublisher) {
        this.inquiryMapper = inquiryMapper;
        this.inquiryEventPublisher = inquiryEventPublisher;
    }

    public List<InquiryView> list() {
        return inquiryMapper.findByTenantId(TenantContext.tenantId());
    }

    public InquiryView create(CreateInquiryRequest request) {
        InquiryView inquiry = new InquiryView("inq-" + UUID.randomUUID(), TenantContext.tenantId(),
                request.customerId(), request.subject(), request.content(), "PENDING_AI", Instant.now().toString());
        inquiryMapper.save(inquiry);
        inquiryEventPublisher.publishCreated(inquiry);
        return inquiry;
    }

    public Optional<InquiryView> updateStatus(String id, String status) {
        return inquiryMapper.findByTenantIdAndId(TenantContext.tenantId(), id)
                .map(item -> inquiryMapper.save(new InquiryView(item.id(), item.tenantId(), item.customerId(),
                        item.subject(), item.content(), status, item.createdAt())));
    }
}