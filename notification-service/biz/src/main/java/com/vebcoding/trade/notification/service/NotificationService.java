package com.vebcoding.trade.notification.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.notification.api.CreateNotificationRequest;
import com.vebcoding.trade.notification.api.NotificationView;
import com.vebcoding.trade.notification.mapper.NotificationMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
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
}
