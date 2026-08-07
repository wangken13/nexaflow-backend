package com.vebcoding.trade.task.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.task.api.CreateTaskRequest;
import com.vebcoding.trade.task.api.DailyReport;
import com.vebcoding.trade.task.api.TaskView;
import com.vebcoding.trade.task.service.TaskService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ApiResponse<List<TaskView>> list() {
        return ApiResponse.ok(taskService.list());
    }

    @PostMapping
    public ApiResponse<TaskView> create(@RequestBody CreateTaskRequest request) {
        return ApiResponse.ok(taskService.create(request));
    }

    @PatchMapping("/{id}/done")
    public ApiResponse<TaskView> complete(@PathVariable String id) {
        return taskService.complete(id)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("任务不存在"));
    }

    @GetMapping("/daily-report")
    public ApiResponse<DailyReport> dailyReport() {
        return ApiResponse.ok(taskService.dailyReport());
    }
}
