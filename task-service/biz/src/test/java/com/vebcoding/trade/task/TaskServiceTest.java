package com.vebcoding.trade.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.task.api.CreateTaskRequest;
import com.vebcoding.trade.task.mapper.InMemoryTaskMapper;
import com.vebcoding.trade.task.service.OperationalMetricsProvider;
import com.vebcoding.trade.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class TaskServiceTest {
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
    void dailyReportUsesCurrentTaskData() {
        TaskService service = new TaskService(new InMemoryTaskMapper());
        var task = service.create(new CreateTaskRequest("Confirm quote", "HIGH", ""));

        assertThat(service.dailyReport().openTasks()).isGreaterThanOrEqualTo(2);

        service.complete(task.id());

        assertThat(service.dailyReport().openTasks()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void dailyReportUsesOperationalMetricsAndPrioritizesOverdueWork() {
        OperationalMetricsProvider metrics = tenantId -> new OperationalMetricsProvider.Metrics(6, 2, 3, 4);
        TaskService service = new TaskService(new InMemoryTaskMapper(), metrics);

        var report = service.dailyReport();

        assertThat(report.newInquiries()).isEqualTo(6);
        assertThat(report.riskyOrders()).isEqualTo(2);
        assertThat(report.pendingApprovals()).isEqualTo(3);
        assertThat(report.overdueTasks()).isEqualTo(4);
        assertThat(report.summary()).contains("逾期跟进");
    }

    @Test
    void createRejectsUnsupportedPriority() {
        TaskService service = new TaskService(new InMemoryTaskMapper());

        assertThatThrownBy(() -> service.create(new CreateTaskRequest("Confirm quote", "URGENT_NOW", "")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("任务优先级不合法");
    }

    @Test
    void createKeepsRelatedBusinessReference() {
        TaskService service = new TaskService(new InMemoryTaskMapper());

        var task = service.create(new CreateTaskRequest("Follow inquiry", "HIGH", "2026-08-10T10:00:00",
                "INQUIRY", "inq-001"));

        assertThat(task.relatedType()).isEqualTo("INQUIRY");
        assertThat(task.relatedId()).isEqualTo("inq-001");
    }

    @Test
    void createRejectsInvalidReminderTimeWithBusinessMessage() {
        TaskService service = new TaskService(new InMemoryTaskMapper());

        assertThatThrownBy(() -> service.create(new CreateTaskRequest("Follow inquiry", "HIGH", "tomorrow")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("提醒时间格式必须为 yyyy-MM-ddTHH:mm:ss");
    }
}
