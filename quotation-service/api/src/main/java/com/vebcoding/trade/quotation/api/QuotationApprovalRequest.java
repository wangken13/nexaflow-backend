package com.vebcoding.trade.quotation.api;

import jakarta.validation.constraints.Size;

public record QuotationApprovalRequest(@Size(max = 1000) String comment) {
}
