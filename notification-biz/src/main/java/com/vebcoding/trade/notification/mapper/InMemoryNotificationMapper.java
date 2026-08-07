package com.vebcoding.trade.notification.mapper;

import com.vebcoding.trade.notification.api.NotificationView;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryNotificationMapper implements NotificationMapper {
    private final List<NotificationView> notifications = new CopyOnWriteArrayList<>(List.of(
            new NotificationView("msg-001", "demo-tenant", "报价跟进提醒",
                    "North Star 报价已超过 24 小时未确认", false, Instant.now().toString())));

    @Override
    public List<NotificationView> findByTenantId(String tenantId) {
        return notifications.stream().filter(item -> tenantId.equals(item.tenantId())).toList();
    }

    @Override
    public NotificationView save(NotificationView notification) {
        notifications.removeIf(item -> item.id().equals(notification.id()));
        notifications.add(notification);
        return notification;
    }
}
