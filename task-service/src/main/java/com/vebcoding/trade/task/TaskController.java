package com.vebcoding.trade.task;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.common.TenantContext;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private final List<TaskView> tasks = new CopyOnWriteArrayList<>(List.of(
            new TaskView("tsk-001", "demo-tenant", "跟进 North Star 报价确认", "HIGH", "OPEN", LocalDateTime.now().plusHours(4).toString())));

    @GetMapping
    public ApiResponse<List<TaskView>> list() {
        String tenantId = TenantContext.tenantId();
        return ApiResponse.ok(tasks.stream().filter(item -> tenantId.equals(item.tenantId())).toList());
    }

    @PostMapping
    public ApiResponse<TaskView> create(@RequestBody CreateTaskRequest request) {
        TaskView task = new TaskView("tsk-" + UUID.randomUUID(), TenantContext.tenantId(), request.title(),
                request.priority(), "OPEN", request.dueAt());
        tasks.add(task);
        return ApiResponse.ok(task);
    }

    @PatchMapping("/{id}/done")
    public ApiResponse<TaskView> complete(@PathVariable String id) {
        return tasks.stream().filter(item -> item.id().equals(id)).findFirst()
                .map(item -> ApiResponse.ok(new TaskView(item.id(), item.tenantId(), item.title(), item.priority(), "DONE", item.dueAt())))
                .orElseGet(() -> ApiResponse.fail("任务不存在"));
    }

    @GetMapping("/daily-report")
    public ApiResponse<DailyReport> dailyReport() {
        return ApiResponse.ok(new DailyReport(7, 3, 1, "今日重点：优先跟进高意向报价客户，检查 1 个临期订单。"));
    }

    public record CreateTaskRequest(@NotBlank String title, String priority, String dueAt) {
    }

    public record TaskView(String id, String tenantId, String title, String priority, String status, String dueAt) {
    }

    public record DailyReport(int openTasks, int newInquiries, int riskyOrders, String summary) {
    }
}
