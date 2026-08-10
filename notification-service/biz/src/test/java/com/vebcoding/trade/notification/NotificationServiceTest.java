package com.vebcoding.trade.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.notification.api.CreateNotificationRequest;
import com.vebcoding.trade.notification.mapper.InMemoryNotificationMapper;
import com.vebcoding.trade.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class NotificationServiceTest {
    @BeforeEach
    void authenticate() {
        TenantContext.setTenantId("demo-tenant");
        TenantContext.setUserId("admin");
        TenantContext.setRole("OWNER");
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void createNotificationIsUnread() {
        NotificationService service = new NotificationService(new InMemoryNotificationMapper());

        var notification = service.create(new CreateNotificationRequest("Payment", "Confirm deposit"));

        assertThat(notification.read()).isFalse();
        assertThat(service.list()).extracting("id").contains(notification.id());
    }

    @Test
    void createRejectsBlankContent() {
        NotificationService service = new NotificationService(new InMemoryNotificationMapper());

        assertThatThrownBy(() -> service.create(new CreateNotificationRequest("Payment", " ")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("通知内容不能为空");
    }

    @Test
    void notificationsCanBeMarkedRead() {
        NotificationService service = new NotificationService(new InMemoryNotificationMapper());
        var notification = service.create(new CreateNotificationRequest("Payment", "Confirm deposit"));

        assertThat(service.markRead(notification.id()).read()).isTrue();
        assertThat(service.markAllRead()).isGreaterThanOrEqualTo(1);
        assertThat(service.list()).allMatch(item -> item.read());
    }
}
