package com.vebcoding.trade.notification.mapper;

import com.vebcoding.trade.notification.api.NotificationView;
import com.vebcoding.trade.notification.api.SupportMessageView;
import com.vebcoding.trade.notification.api.SupportTicketView;
import java.util.List;
import java.util.Optional;

public interface NotificationMapper {
    List<NotificationView> findByTenantId(String tenantId);

    Optional<NotificationView> findByTenantIdAndId(String tenantId, String id);

    NotificationView save(NotificationView notification);

    int markAllRead(String tenantId);

    List<SupportTicketView> findTickets(String tenantId, String createdBy);
    Optional<SupportTicketView> findTicket(String tenantId, String id);
    SupportTicketView saveTicket(SupportTicketView ticket);
    SupportMessageView saveTicketMessage(String tenantId, SupportMessageView message);
    List<SupportMessageView> findTicketMessages(String tenantId, String ticketId);
    SupportTicketView updateTicketStatus(String tenantId, String id, String status);
}
