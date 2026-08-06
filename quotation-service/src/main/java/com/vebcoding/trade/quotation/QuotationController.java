package com.vebcoding.trade.quotation;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.common.TenantContext;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quotation")
public class QuotationController {
    private final List<QuotationView> quotations = new CopyOnWriteArrayList<>();

    @GetMapping
    public ApiResponse<List<QuotationView>> list() {
        String tenantId = TenantContext.tenantId();
        return ApiResponse.ok(quotations.stream().filter(item -> tenantId.equals(item.tenantId())).toList());
    }

    @PostMapping
    public ApiResponse<QuotationView> create(@RequestBody CreateQuotationRequest request) {
        QuotationView quotation = new QuotationView("quo-" + UUID.randomUUID(), TenantContext.tenantId(),
                request.customerId(), request.productName(), request.quantity(), request.unitPrice(), "DRAFT",
                Instant.now().toString());
        quotations.add(quotation);
        return ApiResponse.ok(quotation);
    }

    public record CreateQuotationRequest(@NotBlank String customerId, @NotBlank String productName, int quantity,
                                         BigDecimal unitPrice) {
    }

    public record QuotationView(String id, String tenantId, String customerId, String productName, int quantity,
                                BigDecimal unitPrice, String status, String createdAt) {
    }
}
