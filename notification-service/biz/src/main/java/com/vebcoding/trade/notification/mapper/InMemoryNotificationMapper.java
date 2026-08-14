package com.vebcoding.trade.notification.mapper;

import com.vebcoding.trade.notification.api.NotificationView;
import com.vebcoding.trade.notification.api.SupportMessageView;
import com.vebcoding.trade.notification.api.SupportTicketView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryNotificationMapper implements NotificationMapper {
    private final List<NotificationView> notifications = new CopyOnWriteArrayList<>(List.of(
            new NotificationView("msg-001", "demo-tenant", "报价跟进提醒",
                    "North Star 报价已超过 24 小时未确认", false, Instant.now().toString())));
    private final List<SupportTicketView> tickets = new CopyOnWriteArrayList<>();
    private final List<SupportMessageView> ticketMessages = new CopyOnWriteArrayList<>();

    @Override
    public List<NotificationView> findByTenantId(String tenantId) {
        return notifications.stream().filter(item -> tenantId.equals(item.tenantId())).toList();
    }

    @Override
    public Optional<NotificationView> findByTenantIdAndId(String tenantId, String id) {
        return notifications.stream().filter(item -> tenantId.equals(item.tenantId()) && id.equals(item.id())).findFirst();
    }

    @Override
    public NotificationView save(NotificationView notification) {
        notifications.removeIf(item -> item.id().equals(notification.id()));
        notifications.add(notification);
        return notification;
    }

    @Override
    public int markAllRead(String tenantId) {
        int count = 0;
        for (NotificationView item : List.copyOf(notifications)) {
            if (tenantId.equals(item.tenantId()) && !item.read()) {
                save(new NotificationView(item.id(), item.tenantId(), item.title(), item.content(), true, item.createdAt()));
                count++;
            }
        }
        return count;
    }

    public List<SupportTicketView> findTickets(String tenantId, String createdBy) {
        return tickets.stream().filter(item -> tenantId.equals(item.tenantId())
                && (createdBy == null || createdBy.isBlank() || createdBy.equals(item.createdBy()))).toList();
    }
    public Optional<SupportTicketView> findTicket(String tenantId, String id) {
        return tickets.stream().filter(item -> tenantId.equals(item.tenantId()) && id.equals(item.id())).findFirst()
                .map(item -> new SupportTicketView(item.id(), item.tenantId(), item.createdBy(), item.category(),
                        item.priority(), item.subject(), item.description(), item.status(), item.assignedTo(),
                        item.createdAt(), item.updatedAt(), findTicketMessages(tenantId, id)));
    }
    public SupportTicketView saveTicket(SupportTicketView ticket) { tickets.add(ticket); return ticket; }
    public SupportMessageView saveTicketMessage(String tenantId, SupportMessageView message) {
        ticketMessages.add(message); return message;
    }
    public List<SupportMessageView> findTicketMessages(String tenantId, String ticketId) {
        return ticketMessages.stream().filter(item -> ticketId.equals(item.ticketId())).toList();
    }
    public SupportTicketView updateTicketStatus(String tenantId, String id, String status) {
        SupportTicketView current = findTicket(tenantId, id).orElseThrow();
        tickets.removeIf(item -> id.equals(item.id()));
        SupportTicketView updated = new SupportTicketView(current.id(), current.tenantId(), current.createdBy(),
                current.category(), current.priority(), current.subject(), current.description(), status,
                current.assignedTo(), current.createdAt(), Instant.now().toString(), current.messages());
        tickets.add(updated); return updated;
    }
}
