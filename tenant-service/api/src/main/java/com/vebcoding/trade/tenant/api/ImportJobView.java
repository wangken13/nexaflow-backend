package com.vebcoding.trade.tenant.api;

import java.util.List;

public record ImportJobView(String id, String resourceType, String status, int received, int imported, int skipped,
                            String operatorId, String createdAt, String completedAt, List<String> errors) {
}
