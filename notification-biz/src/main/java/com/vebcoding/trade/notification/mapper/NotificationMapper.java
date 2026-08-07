package com.vebcoding.trade.notification.mapper;

import com.vebcoding.trade.notification.api.NotificationView;
import java.util.List;

public interface NotificationMapper {
    List<NotificationView> findByTenantId(String tenantId);

    NotificationView save(NotificationView notification);
}