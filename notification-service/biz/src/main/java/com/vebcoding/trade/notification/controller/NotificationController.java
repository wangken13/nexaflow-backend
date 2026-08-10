package com.vebcoding.trade.notification.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.notification.api.CreateNotificationRequest;
import com.vebcoding.trade.notification.api.NotificationView;
import com.vebcoding.trade.notification.service.NotificationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationView>> list() {
        return ApiResponse.ok(notificationService.list());
    }

    @PostMapping
    public ApiResponse<NotificationView> create(@RequestBody CreateNotificationRequest request) {
        return ApiResponse.ok(notificationService.create(request));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationView> markRead(@PathVariable String id) {
        return ApiResponse.ok(notificationService.markRead(id));
    }

    @PatchMapping("/read-all")
    public ApiResponse<Integer> markAllRead() {
        return ApiResponse.ok(notificationService.markAllRead());
    }
}
