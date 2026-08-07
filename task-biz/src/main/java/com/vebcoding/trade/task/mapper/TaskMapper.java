package com.vebcoding.trade.task.mapper;

import com.vebcoding.trade.task.api.TaskView;
import java.util.List;
import java.util.Optional;

public interface TaskMapper {
    List<TaskView> findByTenantId(String tenantId);

    Optional<TaskView> findByTenantIdAndId(String tenantId, String id);

    TaskView save(TaskView task);
}