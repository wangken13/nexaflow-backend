package com.vebcoding.trade.task.service;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.task.api.CreateTaskRequest;
import com.vebcoding.trade.task.api.DailyReport;
import com.vebcoding.trade.task.api.TaskView;
import com.vebcoding.trade.task.mapper.TaskMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class TaskService {
    private static final List<String> ALLOWED_PRIORITIES = List.of("HIGH", "NORMAL", "LOW");
    private final TaskMapper taskMapper;
    private final OperationalMetricsProvider metricsProvider;

    public TaskService(TaskMapper taskMapper) {
        this(taskMapper, tenantId -> OperationalMetricsProvider.Metrics.empty());
    }

    @Autowired
    public TaskService(TaskMapper taskMapper, OperationalMetricsProvider metricsProvider) {
        this.taskMapper = taskMapper;
        this.metricsProvider = metricsProvider;
    }

    public List<TaskView> list() {
        return taskMapper.findByTenantId(TenantContext.tenantId());
    }

    public TaskView create(CreateTaskRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES", "OPERATOR");
        String title = TextSanitizer.required(request.title(), "任务标题");
        String priority = normalizePriority(request.priority());
        TaskView task = new TaskView("tsk-" + UUID.randomUUID(), TenantContext.tenantId(), title,
                priority, "OPEN", TextSanitizer.optional(request.dueAt()),
                TextSanitizer.optional(request.relatedType()).toUpperCase(), TextSanitizer.optional(request.relatedId()));
        return taskMapper.save(task);
    }

    public Optional<TaskView> complete(String id) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES", "OPERATOR");
        return taskMapper.findByTenantIdAndId(TenantContext.tenantId(), id)
                .map(item -> taskMapper.save(new TaskView(item.id(), item.tenantId(), item.title(), item.priority(),
                        "DONE", item.dueAt(), item.relatedType(), item.relatedId())));
    }

    public DailyReport dailyReport() {
        List<TaskView> tenantTasks = taskMapper.findByTenantId(TenantContext.tenantId());
        int openTasks = (int) tenantTasks.stream().filter(task -> !"DONE".equals(task.status())).count();
        OperationalMetricsProvider.Metrics metrics = metricsProvider.load(TenantContext.tenantId());
        String summary = metrics.overdueTasks() > 0 ? "存在逾期跟进，请优先明确负责人并完成客户响应。"
                : metrics.riskyOrders() > 0 ? "存在交付风险订单，请核对交期并同步客户。"
                : metrics.pendingApprovals() > 0 ? "有报价等待审批，请及时处理以免延误客户。"
                : openTasks > 0 ? "今日重点：完成高优先级客户跟进。"
                : "今日任务已清空，可复盘报价和订单风险。";
        return new DailyReport(openTasks, metrics.newInquiries(), metrics.riskyOrders(),
                metrics.pendingApprovals(), metrics.overdueTasks(), summary);
    }

    private String normalizePriority(String priority) {
        String normalized = TextSanitizer.optional(priority).toUpperCase();
        if (normalized.isBlank()) {
            return "NORMAL";
        }
        if (!ALLOWED_PRIORITIES.contains(normalized)) {
            throw new BusinessException("任务优先级不合法");
        }
        return normalized;
    }
}
