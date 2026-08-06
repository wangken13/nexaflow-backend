package com.vebcoding.trade.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TaskControllerTest {
    @Test
    void dailyReportContainsWorkload() {
        assertThat(new TaskController().dailyReport().data().openTasks()).isGreaterThan(0);
    }
}
