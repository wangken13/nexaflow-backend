package com.vebcoding.trade.notification.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.notification.api.CreateNotificationRequest;
import com.vebcoding.trade.notification.api.NotificationView;
import com.vebcoding.trade.notification.api.AddSupportMessageRequest;
import com.vebcoding.trade.notification.api.CreateSupportTicketRequest;
import com.vebcoding.trade.notification.api.SupportMessageView;
import com.vebcoding.trade.notification.api.SupportTicketView;
import com.vebcoding.trade.notification.mapper.NotificationMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Set<String> TICKET_CATEGORIES = Set.of("PRODUCT", "BILLING", "DATA", "INTEGRATION", "OTHER");
    private static final Set<String> TICKET_PRIORITIES = Set.of("NORMAL", "HIGH", "URGENT");
    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    public List<NotificationView> list() {
        return notificationMapper.findByTenantId(TenantContext.tenantId());
    }

    public NotificationView create(CreateNotificationRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "OPERATOR");
        String title = TextSanitizer.required(request.title(), "通知标题");
        String content = TextSanitizer.required(request.content(), "通知内容");
        NotificationView notification = new NotificationView("msg-" + UUID.randomUUID(), TenantContext.tenantId(),
                title, content, false, Instant.now().toString());
        return notificationMapper.save(notification);
    }

    public NotificationView markRead(String id) {
        NotificationView current = notificationMapper.findByTenantIdAndId(TenantContext.tenantId(), id)
                .orElseThrow(() -> BusinessException.notFound("消息不存在"));
        if (current.read()) return current;
        return notificationMapper.save(new NotificationView(current.id(), current.tenantId(), current.title(),
                current.content(), true, current.createdAt()));
    }

    public int markAllRead() {
        return notificationMapper.markAllRead(TenantContext.tenantId());
    }

    public List<SupportTicketView> tickets() {
        String createdBy = Set.of("OWNER", "ADMIN").contains(TenantContext.role()) ? "" : TenantContext.userId();
        return notificationMapper.findTickets(TenantContext.tenantId(), createdBy);
    }

    public SupportTicketView ticket(String id) {
        SupportTicketView ticket = notificationMapper.findTicket(TenantContext.tenantId(), id)
                .orElseThrow(() -> BusinessException.notFound("服务工单不存在"));
        requireTicketAccess(ticket);
        return ticket;
    }

    public SupportTicketView createTicket(CreateSupportTicketRequest request) {
        String category = normalize(request.category(), TICKET_CATEGORIES, "工单分类");
        String priority = normalize(request.priority(), TICKET_PRIORITIES, "工单优先级");
        Instant now = Instant.now();
        SupportTicketView ticket = new SupportTicketView("ticket-" + UUID.randomUUID(), TenantContext.tenantId(),
                TenantContext.userId(), category, priority, TextSanitizer.required(request.subject(), "问题主题"),
                TextSanitizer.required(request.description(), "问题描述"), "OPEN", "", now.toString(),
                now.toString(), List.of());
        return notificationMapper.saveTicket(ticket);
    }

    public SupportMessageView addTicketMessage(String id, AddSupportMessageRequest request) {
        SupportTicketView ticket = ticket(id);
        if (ticket.status().equals("CLOSED")) throw BusinessException.conflict("工单已关闭，无法继续回复");
        SupportMessageView message = new SupportMessageView("reply-" + UUID.randomUUID(), id,
                TenantContext.userId(), TextSanitizer.required(request.content(), "回复内容"), Instant.now().toString());
        return notificationMapper.saveTicketMessage(TenantContext.tenantId(), message);
    }

    public SupportTicketView closeTicket(String id) {
        SupportTicketView ticket = ticket(id);
        if (ticket.status().equals("CLOSED")) return ticket;
        return notificationMapper.updateTicketStatus(TenantContext.tenantId(), id, "CLOSED");
    }

    private void requireTicketAccess(SupportTicketView ticket) {
        if (!Set.of("OWNER", "ADMIN").contains(TenantContext.role())
                && !ticket.createdBy().equals(TenantContext.userId())) {
            throw new com.vebcoding.trade.common.AccessDeniedException("当前账号无权查看该服务工单");
        }
    }

    private String normalize(String value, Set<String> allowed, String field) {
        String normalized = TextSanitizer.required(value, field).toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw new BusinessException(field + "不合法");
        return normalized;
    }
}
