package com.vebcoding.trade.common;

import java.util.List;
import java.util.UUID;

public interface ImportJobRecorder {
    String start(String resourceType, int received);

    BulkImportResult complete(String jobId, int received, int imported, List<String> errors);

    static ImportJobRecorder passthrough() {
        return new ImportJobRecorder() {
            public String start(String resourceType, int received) { return "test-" + UUID.randomUUID(); }
            public BulkImportResult complete(String jobId, int received, int imported, List<String> errors) {
                return new BulkImportResult(jobId, received, imported, received - imported, List.copyOf(errors));
            }
        };
    }
}
