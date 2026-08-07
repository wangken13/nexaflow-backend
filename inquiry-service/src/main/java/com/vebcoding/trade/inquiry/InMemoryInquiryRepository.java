package com.vebcoding.trade.inquiry;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryInquiryRepository implements InquiryRepository {
    private final List<InquiryController.InquiryView> inquiries = new CopyOnWriteArrayList<>();

    @Override
    public List<InquiryController.InquiryView> findByTenantId(String tenantId) {
        return inquiries.stream().filter(item -> tenantId.equals(item.tenantId())).toList();
    }

    @Override
    public InquiryController.InquiryView save(InquiryController.InquiryView inquiry) {
        inquiries.removeIf(item -> item.id().equals(inquiry.id()));
        inquiries.add(inquiry);
        return inquiry;
    }

    @Override
    public Optional<InquiryController.InquiryView> findByTenantIdAndId(String tenantId, String id) {
        return inquiries.stream()
                .filter(item -> tenantId.equals(item.tenantId()))
                .filter(item -> item.id().equals(id))
                .findFirst();
    }
}
