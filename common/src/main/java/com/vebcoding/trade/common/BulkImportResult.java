package com.vebcoding.trade.common;

import java.util.List;

public record BulkImportResult(int received, int imported, int skipped, List<String> errors) {
}
