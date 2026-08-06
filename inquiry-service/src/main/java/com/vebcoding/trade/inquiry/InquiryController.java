package com.vebcoding.trade.inquiry;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.common.TenantContext;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inquiry")
public class InquiryController {
    private final ObjectProvider<RabbitTemplate> rabbitTemplate;
    private final List<InquiryView> inquiries = new CopyOnWriteArrayList<>();

    public InquiryController(ObjectProvider<RabbitTemplate> rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping
    public ApiResponse<List<InquiryView>> list() {
        String tenantId = TenantContext.tenantId();
        return ApiResponse.ok(inquiries.stream().filter(item -> tenantId.equals(item.tenantId())).toList());
    }

    @PostMapping
    public ApiResponse<InquiryView> create(@RequestBody CreateInquiryRequest request) {
        InquiryView inquiry = new InquiryView("inq-" + UUID.randomUUID(), TenantContext.tenantId(), request.customerId(),
                request.subject(), request.content(), "PENDING_AI", Instant.now().toString());
        inquiries.add(inquiry);
        rabbitTemplate.ifAvailable(template -> template.convertAndSend("trade.ai", "ai.analysis.requested",
                Map.of("inquiryId", inquiry.id(), "tenantId", inquiry.tenantId(), "content", inquiry.content())));
        return ApiResponse.ok(inquiry);
    }

    @PatchMapping("/{id}/status/{status}")
    public ApiResponse<InquiryView> updateStatus(@PathVariable String id, @PathVariable String status) {
        return inquiries.stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .map(item -> ApiResponse.ok(new InquiryView(item.id(), item.tenantId(), item.customerId(), item.subject(),
                        item.content(), status, item.createdAt())))
                .orElseGet(() -> ApiResponse.fail("询盘不存在"));
    }

    public record CreateInquiryRequest(@NotBlank String customerId, @NotBlank String subject, @NotBlank String content) {
    }

    public record InquiryView(String id, String tenantId, String customerId, String subject, String content, String status,
                              String createdAt) {
    }

}
