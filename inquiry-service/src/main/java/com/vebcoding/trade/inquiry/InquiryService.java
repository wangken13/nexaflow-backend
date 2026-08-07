package com.vebcoding.trade.inquiry;

import com.vebcoding.trade.common.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InquiryService {
    private final InquiryRepository inquiryRepository;
    private final InquiryEventPublisher inquiryEventPublisher;

    public InquiryService(InquiryRepository inquiryRepository, InquiryEventPublisher inquiryEventPublisher) {
        this.inquiryRepository = inquiryRepository;
        this.inquiryEventPublisher = inquiryEventPublisher;
    }

    public List<InquiryController.InquiryView> list() {
        return inquiryRepository.findByTenantId(TenantContext.tenantId());
    }

    public InquiryController.InquiryView create(InquiryController.CreateInquiryRequest request) {
        InquiryController.InquiryView inquiry = new InquiryController.InquiryView("inq-" + UUID.randomUUID(),
                TenantContext.tenantId(), request.customerId(), request.subject(), request.content(), "PENDING_AI",
                Instant.now().toString());
        inquiryRepository.save(inquiry);
        inquiryEventPublisher.publishCreated(inquiry);
        return inquiry;
    }

    public Optional<InquiryController.InquiryView> updateStatus(String id, String status) {
        return inquiryRepository.findByTenantIdAndId(TenantContext.tenantId(), id)
                .map(item -> inquiryRepository.save(new InquiryController.InquiryView(item.id(), item.tenantId(),
                        item.customerId(), item.subject(), item.content(), status, item.createdAt())));
    }
}
