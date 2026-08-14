package com.vebcoding.trade.customer.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CustomerBulkImportRequest(@NotEmpty @Size(max = 500) List<@Valid CreateCustomerRequest> rows) {
}
