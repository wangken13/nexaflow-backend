package com.vebcoding.trade.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.task.controller.TaskController;
import com.vebcoding.trade.task.mapper.InMemoryTaskMapper;
import com.vebcoding.trade.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class TaskControllerTest {
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
    void dailyReportContainsWorkload() {
        TaskService service = new TaskService(new InMemoryTaskMapper());

        assertThat(new TaskController(service).dailyReport().data().openTasks()).isGreaterThan(0);
    }
}
