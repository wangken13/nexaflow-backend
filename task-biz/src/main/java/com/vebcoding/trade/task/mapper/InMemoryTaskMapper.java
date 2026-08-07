package com.vebcoding.trade.task.mapper;

import com.vebcoding.trade.task.api.TaskView;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTaskMapper implements TaskMapper {
    private final List<TaskView> tasks = new CopyOnWriteArrayList<>(List.of(
            new TaskView("tsk-001", "demo-tenant", "跟进 North Star 报价确认", "HIGH", "OPEN",
                    LocalDateTime.now().plusHours(4).toString())));

    @Override
    public List<TaskView> findByTenantId(String tenantId) {
        return tasks.stream().filter(item -> tenantId.equals(item.tenantId())).toList();
    }

    @Override
    public Optional<TaskView> findByTenantIdAndId(String tenantId, String id) {
        return tasks.stream()
                .filter(item -> tenantId.equals(item.tenantId()))
                .filter(item -> item.id().equals(id))
                .findFirst();
    }

    @Override
    public TaskView save(TaskView task) {
        tasks.removeIf(item -> item.id().equals(task.id()));
        tasks.add(task);
        return task;
    }
}
