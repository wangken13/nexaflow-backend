package com.vebcoding.trade.notification.mapper;

import com.vebcoding.trade.notification.api.NotificationView;
import java.util.List;
import java.util.Optional;

public interface NotificationMapper {
    List<NotificationView> findByTenantId(String tenantId);

    Optional<NotificationView> findByTenantIdAndId(String tenantId, String id);

    NotificationView save(NotificationView notification);

    int markAllRead(String tenantId);
}
