package com.vebcoding.trade.notification.service;

import com.vebcoding.trade.common.TenantContext;
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
        NotificationView notification = new NotificationView("msg-" + UUID.randomUUID(), TenantContext.tenantId(),
                request.title(), request.content(), false, Instant.now().toString());
        return notificationMapper.save(notification);
    }
}