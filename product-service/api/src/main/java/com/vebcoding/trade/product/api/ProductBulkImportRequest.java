package com.vebcoding.trade.product.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductBulkImportRequest(@NotEmpty @Size(max = 500) List<@Valid UpsertProductRequest> rows) {
}
