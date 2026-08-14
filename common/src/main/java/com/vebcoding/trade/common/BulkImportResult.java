package com.vebcoding.trade.common;

import java.util.List;

public record BulkImportResult(String jobId, int received, int imported, int skipped, List<String> errors) {
    public BulkImportResult(int received, int imported, int skipped, List<String> errors) {
        this("", received, imported, skipped, errors);
    }
}
