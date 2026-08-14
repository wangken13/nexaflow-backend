package com.vebcoding.trade.inquiry.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.inquiry.api.InboundInquiryResponse;
import com.vebcoding.trade.inquiry.service.InboundInquiryService;
import com.vebcoding.trade.inquiry.service.IntegrationInvocationLogger;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inquiry/inbound")
public class InboundInquiryController {
    private final InboundInquiryService inboundInquiryService;
    private final IntegrationInvocationLogger invocationLogger;

    public InboundInquiryController(InboundInquiryService inboundInquiryService,
                                    IntegrationInvocationLogger invocationLogger) {
        this.inboundInquiryService = inboundInquiryService;
        this.invocationLogger = invocationLogger;
    }

    @PostMapping("/{credentialId}")
    public ApiResponse<InboundInquiryResponse> accept(
            @PathVariable String credentialId,
            @RequestHeader("X-Nexa-Timestamp") String timestamp,
            @RequestHeader("X-Nexa-Signature") String signature,
            @RequestBody String body, HttpServletRequest servletRequest) {
        long startedAt = System.nanoTime();
        String requestId = requestId(servletRequest);
        try {
            InboundInquiryResponse result = inboundInquiryService.accept(credentialId, timestamp, signature, body);
            invocationLogger.record(credentialId, requestId, "POST", servletRequest.getRequestURI(),
                    clientIp(servletRequest), result.duplicate() ? "DUPLICATE" : "ACCEPTED", elapsed(startedAt));
            return ApiResponse.ok(result);
        } catch (RuntimeException exception) {
            invocationLogger.record(credentialId, requestId, "POST", servletRequest.getRequestURI(),
                    clientIp(servletRequest), "REJECTED", elapsed(startedAt));
            throw exception;
        }
    }

    private long elapsed(long startedAt) { return (System.nanoTime() - startedAt) / 1_000_000; }
    private String requestId(HttpServletRequest request) {
        String value = request.getHeader("X-Request-Id");
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.substring(0, Math.min(64, value.length()));
    }
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String value = forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        return value == null ? "unknown" : value.substring(0, Math.min(64, value.length()));
    }
}
