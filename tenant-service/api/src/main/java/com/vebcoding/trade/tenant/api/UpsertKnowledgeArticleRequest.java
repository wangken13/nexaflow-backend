package com.vebcoding.trade.tenant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertKnowledgeArticleRequest(@NotBlank @Size(max = 200) String title,
                                            @NotBlank @Size(max = 32) String category,
                                            @NotBlank @Size(max = 20_000) String content,
                                            boolean active) {
}
