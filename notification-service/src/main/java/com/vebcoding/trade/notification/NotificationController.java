package com.vebcoding.trade.notification;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.common.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification")
public class NotificationController {
    private final List<NotificationView> notifications = new CopyOnWriteArrayList<>(List.of(
            new NotificationView("msg-001", "demo-tenant", "报价跟进提醒", "North Star 报价已超过 24 小时未确认", false, Instant.now().toString())));

    @GetMapping
    public ApiResponse<List<NotificationView>> list() {
        String tenantId = TenantContext.tenantId();
        return ApiResponse.ok(notifications.stream().filter(item -> tenantId.equals(item.tenantId())).toList());
    }

    @PostMapping
    public ApiResponse<NotificationView> create(@RequestBody CreateNotificationRequest request) {
        NotificationView notification = new NotificationView("msg-" + UUID.randomUUID(), TenantContext.tenantId(),
                request.title(), request.content(), false, Instant.now().toString());
        notifications.add(notification);
        return ApiResponse.ok(notification);
    }

    public record CreateNotificationRequest(String title, String content) {
    }

    public record NotificationView(String id, String tenantId, String title, String content, boolean read, String createdAt) {
    }
}
