package com.vebcoding.trade.tenant.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInvoiceRequest(@NotBlank @Size(max = 255) String invoiceTitle,
                                   @Size(max = 64) String taxNumber,
                                   @NotBlank @Email @Size(max = 255) String recipientEmail) {
}
