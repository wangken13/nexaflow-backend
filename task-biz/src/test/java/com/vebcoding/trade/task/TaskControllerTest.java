package com.vebcoding.trade.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.task.controller.TaskController;
import com.vebcoding.trade.task.mapper.InMemoryTaskMapper;
import com.vebcoding.trade.task.service.TaskService;
import org.junit.jupiter.api.Test;

class TaskControllerTest {
    @Test
    void dailyReportContainsWorkload() {
        TaskService service = new TaskService(new InMemoryTaskMapper());

        assertThat(new TaskController(service).dailyReport().data().openTasks()).isGreaterThan(0);
    }
}
