package com.vebcoding.trade.task.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.task.api.CreateTaskRequest;
import com.vebcoding.trade.task.api.DailyReport;
import com.vebcoding.trade.task.api.TaskView;
import com.vebcoding.trade.task.mapper.TaskMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
    private final TaskMapper taskMapper;

    public TaskService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public List<TaskView> list() {
        return taskMapper.findByTenantId(TenantContext.tenantId());
    }

    public TaskView create(CreateTaskRequest request) {
        TaskView task = new TaskView("tsk-" + UUID.randomUUID(), TenantContext.tenantId(), request.title(),
                request.priority(), "OPEN", request.dueAt());
        return taskMapper.save(task);
    }

    public Optional<TaskView> complete(String id) {
        return taskMapper.findByTenantIdAndId(TenantContext.tenantId(), id)
                .map(item -> taskMapper.save(new TaskView(item.id(), item.tenantId(), item.title(), item.priority(),
                        "DONE", item.dueAt())));
    }

    public DailyReport dailyReport() {
        return new DailyReport(7, 3, 1, "今日重点：优先跟进高意向报价客户，检查临期订单。");
    }
}
