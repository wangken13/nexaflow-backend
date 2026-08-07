package com.vebcoding.trade.inquiry.mapper;

import com.vebcoding.trade.inquiry.api.InquiryView;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryInquiryMapper implements InquiryMapper {
    private final List<InquiryView> inquiries = new CopyOnWriteArrayList<>();

    @Override
    public List<InquiryView> findByTenantId(String tenantId) {
        return inquiries.stream().filter(item -> tenantId.equals(item.tenantId())).toList();
    }

    @Override
    public InquiryView save(InquiryView inquiry) {
        inquiries.removeIf(item -> item.id().equals(inquiry.id()));
        inquiries.add(inquiry);
        return inquiry;
    }

    @Override
    public Optional<InquiryView> findByTenantIdAndId(String tenantId, String id) {
        return inquiries.stream()
                .filter(item -> tenantId.equals(item.tenantId()))
                .filter(item -> item.id().equals(id))
                .findFirst();
    }
}