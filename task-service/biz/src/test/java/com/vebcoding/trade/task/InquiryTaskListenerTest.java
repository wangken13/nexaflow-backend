package com.vebcoding.trade.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.inquiry.api.InquiryCreatedEvent;
import com.vebcoding.trade.task.mapper.InMemoryTaskMapper;
import com.vebcoding.trade.task.service.InquiryTaskListener;
import com.vebcoding.trade.task.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class InquiryTaskListenerTest {
    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void createsTenantScopedFollowupTaskForNewInquiryEvent() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), org.mockito.ArgumentMatchers.eq("evt-1"))).thenReturn(1);
        InMemoryTaskMapper mapper = new InMemoryTaskMapper();
        InquiryTaskListener listener = new InquiryTaskListener(new TaskService(mapper), jdbc);

        listener.createFollowupTask(new InquiryCreatedEvent("evt-1", "inq-1", "tenant-1", "Need quote"));

        TenantContext.setTenantId("tenant-1");
        TenantContext.setUserId("owner");
        TenantContext.setRole("OWNER");
        assertThat(new TaskService(mapper).list()).anySatisfy(task -> {
            assertThat(task.relatedType()).isEqualTo("INQUIRY");
            assertThat(task.relatedId()).isEqualTo("inq-1");
            assertThat(task.priority()).isEqualTo("HIGH");
        });
    }

    @Test
    void duplicateEventDoesNotCreateSecondTask() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), org.mockito.ArgumentMatchers.eq("evt-1"))).thenReturn(0);
        InMemoryTaskMapper mapper = new InMemoryTaskMapper();

        new InquiryTaskListener(new TaskService(mapper), jdbc)
                .createFollowupTask(new InquiryCreatedEvent("evt-1", "inq-1", "tenant-1", "Need quote"));

        TenantContext.setTenantId("tenant-1");
        TenantContext.setUserId("owner");
        TenantContext.setRole("OWNER");
        assertThat(new TaskService(mapper).list()).isEmpty();
    }
}
