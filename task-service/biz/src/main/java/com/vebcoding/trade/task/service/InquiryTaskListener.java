package com.vebcoding.trade.task.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.inquiry.api.InquiryCreatedEvent;
import com.vebcoding.trade.task.api.CreateTaskRequest;
import java.time.Instant;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InquiryTaskListener {
    private final TaskService taskService;
    private final JdbcTemplate jdbcTemplate;

    public InquiryTaskListener(TaskService taskService, JdbcTemplate jdbcTemplate) {
        this.taskService = taskService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    @RabbitListener(queues = "trade.task.inquiry")
    public void createFollowupTask(InquiryCreatedEvent event) {
        if (jdbcTemplate.update("""
                INSERT IGNORE INTO message_consumption (consumer_name, event_id) VALUES ('inquiry-task', ?)
                """, event.eventId()) != 1) return;
        try {
            TenantContext.setTenantId(event.tenantId());
            TenantContext.setUserId("system-task");
            TenantContext.setRole("OWNER");
            taskService.create(new CreateTaskRequest("处理新询盘", "HIGH",
                    Instant.now().plusSeconds(4 * 3600).toString(), "INQUIRY", event.inquiryId()));
        } finally {
            TenantContext.clear();
        }
    }
}
