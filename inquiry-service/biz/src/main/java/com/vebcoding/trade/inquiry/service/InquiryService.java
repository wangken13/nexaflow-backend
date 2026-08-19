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
        InquiryView inquiry = createPending(TenantContext.tenantId(), request.customerId(), request.subject(),
                request.content(), "MANUAL", "", TenantContext.userId());
        if (!request.streamAnalysisRequested()) {
            inquiryEventPublisher.publishCreated(inquiry);
        }
        return inquiry;
    }

    InquiryView createFromChannel(String tenantId, String customerId, String subject, String content,
                                  String sourceChannel, String externalId) {
        InquiryView inquiry = createPending(tenantId, customerId, subject, content, sourceChannel, externalId, "");
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
                        item.subject(), item.content(), status, item.sourceChannel(), item.externalId(), item.ownerId(),
                        item.nextActionDue(), item.createdAt())));
    }

    private InquiryView createPending(String tenantId, String customerId, String subject, String content,
                                      String sourceChannel, String externalId, String ownerId) {
        String normalizedCustomerId = TextSanitizer.required(customerId, "客户ID");
        if (!inquiryMapper.customerExists(tenantId, normalizedCustomerId)) {
            throw BusinessException.notFound("客户不存在或不属于当前企业");
        }
        InquiryView inquiry = new InquiryView("inq-" + UUID.randomUUID(), tenantId,
                normalizedCustomerId, TextSanitizer.required(subject, "询盘主题"),
                TextSanitizer.required(content, "询盘内容"), "PENDING_AI", sourceChannel, externalId, ownerId,
                Instant.now().plusSeconds(4 * 3600).toString(), Instant.now().toString());
        return inquiryMapper.save(inquiry);
    }
}
