package com.vebcoding.trade.task.api;

import jakarta.validation.constraints.NotBlank;

public record CreateTaskRequest(@NotBlank String title, String priority, String dueAt) {
}