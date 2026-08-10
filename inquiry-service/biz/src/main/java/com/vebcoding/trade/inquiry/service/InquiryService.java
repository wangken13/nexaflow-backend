package com.vebcoding.trade.inquiry.service;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.inquiry.api.CreateInquiryRequest;
import com.vebcoding.trade.inquiry.api.InquiryView;
import com.vebcoding.trade.inquiry.mapper.InquiryMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InquiryService {
    private static final List<String> ALLOWED_STATUSES = List.of("PENDING_AI", "ANALYZED", "QUOTED", "FOLLOWING", "CLOSED");
    private final InquiryMapper inquiryMapper;
    private final InquiryEventPublisher inquiryEventPublisher;

    public InquiryService(InquiryMapper inquiryMapper, InquiryEventPublisher inquiryEventPublisher) {
        this.inquiryMapper = inquiryMapper;
        this.inquiryEventPublisher = inquiryEventPublisher;
    }

    public List<InquiryView> list() {
        return inquiryMapper.findByTenantId(TenantContext.tenantId());
    }

    public InquiryView get(String id) {
        return inquiryMapper.findByTenantIdAndId(TenantContext.tenantId(), id)
                .orElseThrow(() -> BusinessException.notFound("询盘不存在"));
    }

    @Transactional
    public InquiryView create(CreateInquiryRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        String customerId = TextSanitizer.required(request.customerId(), "客户ID");
        String subject = TextSanitizer.required(request.subject(), "询盘主题");
        String content = TextSanitizer.required(request.content(), "询盘内容");
        InquiryView inquiry = new InquiryView("inq-" + UUID.randomUUID(), TenantContext.tenantId(),
                customerId, subject, content, "PENDING_AI", Instant.now().toString());
        inquiryMapper.save(inquiry);
        inquiryEventPublisher.publishCreated(inquiry);
        return inquiry;
    }

    public Optional<InquiryView> updateStatus(String id, String status) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new BusinessException("询盘状态不合法");
        }
        return inquiryMapper.findByTenantIdAndId(TenantContext.tenantId(), id)
                .map(item -> inquiryMapper.save(new InquiryView(item.id(), item.tenantId(), item.customerId(),
                        item.subject(), item.content(), status, item.createdAt())));
    }
}
